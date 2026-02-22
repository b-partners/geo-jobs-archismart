package app.bpartners.geojobs.service.event;

import static app.bpartners.geojobs.endpoint.rest.controller.mapper.FeatureMapper.toDomainFeature;
import static app.bpartners.geojobs.service.event.DetectionRoofSlopeAndHeightRequestedService.*;
import static app.bpartners.geojobs.service.lidar.model.LidarDataStatus.AVAILABLE;
import static java.time.Instant.now;
import static java.util.UUID.randomUUID;
import static java.util.stream.Collectors.toSet;

import app.bpartners.geojobs.endpoint.event.model.FeatureRoofSlopeAndHeightRequested;
import app.bpartners.geojobs.endpoint.event.model.FeatureVggRequested;
import app.bpartners.geojobs.endpoint.rest.controller.mapper.FeatureMapper;
import app.bpartners.geojobs.model.lidar.planes.Plane3D;
import app.bpartners.geojobs.repository.DetectionRepository;
import app.bpartners.geojobs.repository.FeatureDelimitationComputingRepository;
import app.bpartners.geojobs.repository.model.detection.FeatureWithDelimitation;
import app.bpartners.geojobs.repository.model.feature.FeatureDelimitationComputing;
import app.bpartners.geojobs.service.lidar.LidarRoofsAnalysisProcessor;
import java.util.*;
import java.util.function.Consumer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.locationtech.jts.geom.Geometry;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class FeatureRoofSlopeAndHeightRequestedService
    implements Consumer<FeatureRoofSlopeAndHeightRequested> {
  private final DetectionRepository detectionRepository;
  private final LidarRoofsAnalysisProcessor lidarRoofsAnalysisProcessor;
  private final FeatureMapper featureMapper;
  private final FeatureVggRequestedService zoneVggRequestedService;
  private final FeatureDelimitationComputingRepository featureDelimitationComputingRepository;

  @Override
  public void accept(FeatureRoofSlopeAndHeightRequested featureRoofSlopeAndHeightRequested) {
    var detectionIdentifier = featureRoofSlopeAndHeightRequested.getDetectionIdentifier();
    var feature = featureRoofSlopeAndHeightRequested.getFeature();
    int featureNb = featureRoofSlopeAndHeightRequested.getFeatureNb();

    var detection = detectionRepository.findById(detectionIdentifier).orElseThrow();
    var featureId =
        feature.getProperties().get("feature_id") != null
            ? feature.getProperties().get("feature_id").toString()
            : null;
    var featureWithDelimitationList = detection.getFeatureWithDelimitations();
    var filteredDelimitations =
        featureWithDelimitationList.stream()
            .filter(
                featureWithDelimitation ->
                    featureWithDelimitation.getRestFeature().getProperties().get("feature_id")
                            != null
                        && featureWithDelimitation
                            .getRestFeature()
                            .getProperties()
                            .get("feature_id")
                            .toString()
                            .equalsIgnoreCase(featureId))
            .map(FeatureWithDelimitation::delimitations)
            .filter(Objects::nonNull)
            .flatMap(List::stream)
            .toList();
    var delimitationsFilteredByFeature =
        new FeatureWithDelimitation(toDomainFeature(feature), filteredDelimitations);

    if (isAlreadyProcessedAsSuccess(delimitationsFilteredByFeature)) {
      log.warn(
          "Detection(id={}) for feature={} lidar properties has already been processed",
          detectionIdentifier,
          feature);
      return;
    }

    var roofGeometries = toGeometries(delimitationsFilteredByFeature);
    var roofsAnalysesResult = lidarRoofsAnalysisProcessor.from(roofGeometries);

    var featureDelimitationWithComputedProperties =
        addRoofProperties(delimitationsFilteredByFeature, roofsAnalysesResult);

    featureDelimitationComputingRepository.save(
        FeatureDelimitationComputing.builder()
            .id(randomUUID().toString())
            .featurePropertiesIdentifier(featureId)
            .detectionIdentifier(detectionIdentifier)
            .featureWithDelimitation(featureDelimitationWithComputedProperties)
            .creationDatetime(now())
            .build());

    zoneVggRequestedService.accept(
        new FeatureVggRequested(detectionIdentifier, feature, featureNb));
  }

  private boolean isAlreadyProcessedAsSuccess(FeatureWithDelimitation featureWithDelimitation) {
    return featureWithDelimitation.delimitations().stream()
        .anyMatch(
            feature ->
                feature.getProperties() != null
                    && AVAILABLE.equals(
                        feature.getProperties().get(LIDAR_DATA_STATUS_PROPERTY_NAME)));
  }

  private Set<Geometry> toGeometries(FeatureWithDelimitation featureWithDelimitation) {
    Set<app.bpartners.geojobs.repository.model.Feature> flattedFeatures =
        new HashSet<>(featureWithDelimitation.delimitations());
    return flattedFeatures.stream().map(featureMapper::domainToGeometry).collect(toSet());
  }

  private FeatureWithDelimitation addRoofProperties(
      FeatureWithDelimitation featureWithDelimitation,
      LidarRoofsAnalysisProcessor.RoofsAnalysisResult roofsAnalysisResult) {
    for (var delimitation : featureWithDelimitation.delimitations()) {
      if (delimitation.getProperties() == null) {
        delimitation.setProperties(new HashMap<>());
      }

      var properties = delimitation.getProperties();
      var roofProperties =
          roofsAnalysisResult.getProperties(featureMapper.domainToGeometry(delimitation));

      var planes = roofProperties.getRoofPlanes();
      var firstPlane = planes.isEmpty() ? Plane3D.empty() : planes.getFirst();
      properties.put(ROOF_SLOPE_PROPERTY_NAME, firstPlane.getSlopeInDegrees().getValue());
      properties.put(ROOF_HEIGHT_PROPERTY_NAME, roofProperties.getHeightInMeters().getValue());
      properties.put(LIDAR_DATA_STATUS_PROPERTY_NAME, roofProperties.getData().status());
    }
    return featureWithDelimitation;
  }
}
