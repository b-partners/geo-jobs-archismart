package app.bpartners.geojobs.service.event;

import static app.bpartners.geojobs.endpoint.rest.controller.v1.mapper.FeatureMapper.toDomainFeature;
import static app.bpartners.geojobs.service.event.DetectionRoofSlopeAndHeightRequestedService.*;
import static app.bpartners.geojobs.service.lidar.model.LidarDataStatus.AVAILABLE;
import static java.util.stream.Collectors.toSet;

import app.bpartners.geojobs.endpoint.event.EventProducer;
import app.bpartners.geojobs.endpoint.event.model.FeatureRoofSlopeAndHeightRequested;
import app.bpartners.geojobs.endpoint.event.model.FeatureVggRequested;
import app.bpartners.geojobs.endpoint.rest.controller.v1.mapper.FeatureMapper;
import app.bpartners.geojobs.endpoint.rest.model.Feature;
import app.bpartners.geojobs.model.lidar.planes.Plane3D;
import app.bpartners.geojobs.repository.DetectionRepository;
import app.bpartners.geojobs.service.lidar.LidarRoofsAnalysisProcessor;
import jakarta.persistence.EntityManager;
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
  private static final String ROOF_SLOPE_DATA_STATUS_PROPERTY_NAME = "roof_slope_data_status";
  private static final String ROOF_HEIGHT_DATA_STATUS_PROPERTY_NAME = "roof_height_data_status";
  private final DetectionRepository detectionRepository;
  private final LidarRoofsAnalysisProcessor lidarRoofsAnalysisProcessor;
  private final FeatureMapper featureMapper;
  private final EventProducer eventProducer;
  private final EntityManager entityManager;

  @Override
  public void accept(FeatureRoofSlopeAndHeightRequested featureRoofSlopeAndHeightRequested) {
    var detectionIdentifier = featureRoofSlopeAndHeightRequested.getDetectionIdentifier();
    var currentFeature = featureRoofSlopeAndHeightRequested.getFeature();
    entityManager.clear();

    var detection = detectionRepository.findById(detectionIdentifier).orElseThrow();

    var delimitationFeatures = detection.getDelimitationOf(currentFeature);

    if (isAlreadyProcessedAsSuccess(delimitationFeatures)) {
      log.warn(
          "Detection(id={}) for feature={} lidar properties has already been processed",
          detectionIdentifier,
          currentFeature);
      return;
    }

    var roofGeometries = toGeometries(delimitationFeatures);

    var roofsAnalysesResult = lidarRoofsAnalysisProcessor.from(roofGeometries);

    var delimitationFeaturesWithHeightAndSlopeProperties =
        computeHeightAndSlopeProperties(delimitationFeatures, roofsAnalysesResult);

    var domainDelimitationFeaturesWithHeightAndSlopeProperties =
        delimitationFeaturesWithHeightAndSlopeProperties.stream()
            .map(FeatureMapper::toDomainFeature)
            .toList();

    detection.addFeatureDelimitations(
        toDomainFeature(currentFeature), domainDelimitationFeaturesWithHeightAndSlopeProperties);

    detectionRepository.save(detection);

    eventProducer.accept(List.of(new FeatureVggRequested(detectionIdentifier, currentFeature)));
  }

  private boolean isAlreadyProcessedAsSuccess(List<Feature> delimitationFeatures) {
    return delimitationFeatures.stream()
        .anyMatch(
            feature ->
                feature.getProperties() != null
                    && AVAILABLE.equals(
                        feature.getProperties().get(LIDAR_DATA_STATUS_PROPERTY_NAME)));
  }

  private Set<Geometry> toGeometries(List<Feature> delimitationFeatures) {
    Set<app.bpartners.geojobs.repository.model.Feature> flattedFeatures =
        delimitationFeatures.stream()
            .map(FeatureMapper::toDomainFeature)
            .collect(java.util.stream.Collectors.toSet());
    return flattedFeatures.stream().map(featureMapper::domainToGeometry).collect(toSet());
  }

  private List<Feature> computeHeightAndSlopeProperties(
      List<Feature> delimitationFeatures,
      LidarRoofsAnalysisProcessor.RoofsAnalysisResult roofsAnalysisResult) {
    return delimitationFeatures.stream()
        .map(
            delimitation -> {
              var actualProperties = new HashMap<String, Object>();
              if (delimitation.getProperties() != null) {
                actualProperties.putAll(delimitation.getProperties());
              }
              var roofProperties =
                  roofsAnalysisResult.getProperties(
                      featureMapper.domainToGeometry(toDomainFeature(delimitation)));

              var planes = roofProperties.getRoofPlanes();
              var firstPlane = planes.isEmpty() ? Plane3D.empty() : planes.getFirst();
              var slopeValue = firstPlane.getSlopeInDegrees().getValue();
              var heightValue = roofProperties.getHeightInMeters().getValue();
              var lidarDataStatus = roofProperties.getData().status();
              actualProperties.put(ROOF_SLOPE_PROPERTY_NAME, slopeValue);
              actualProperties.put(ROOF_HEIGHT_PROPERTY_NAME, heightValue);
              actualProperties.put(LIDAR_DATA_STATUS_PROPERTY_NAME, lidarDataStatus);
              actualProperties.put(ROOF_SLOPE_DATA_STATUS_PROPERTY_NAME, lidarDataStatus);
              actualProperties.put(ROOF_HEIGHT_DATA_STATUS_PROPERTY_NAME, lidarDataStatus);

              return new Feature()
                  .type(delimitation.getType())
                  .geometry(delimitation.getGeometry())
                  .properties(actualProperties);
            })
        .toList();
  }
}
