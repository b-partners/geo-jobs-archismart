package app.bpartners.geojobs.service.event;

import static app.bpartners.geojobs.endpoint.rest.controller.v1.mapper.FeatureMapper.toDomainFeature;
import static app.bpartners.geojobs.model.lidar.LidarProcessorType.SAFE_MODE;
import static app.bpartners.geojobs.service.event.DetectionRoofSlopeAndHeightRequestedService.*;
import static app.bpartners.geojobs.service.lidar.model.LidarDataStatus.AVAILABLE;
import static app.bpartners.geojobs.service.lidar.model.LidarDataStatus.UNAVAILABLE;
import static java.util.UUID.randomUUID;

import app.bpartners.geojobs.endpoint.event.EventProducer;
import app.bpartners.geojobs.endpoint.event.model.FeatureRoofSlopeAndHeightRequested;
import app.bpartners.geojobs.endpoint.event.model.FeatureVggRequested;
import app.bpartners.geojobs.endpoint.rest.controller.v1.mapper.FeatureMapper;
import app.bpartners.geojobs.endpoint.rest.model.Feature;
import app.bpartners.geojobs.file.bucket.BucketComponent;
import app.bpartners.geojobs.repository.DetectionRepository;
import app.bpartners.geojobs.repository.model.cityjson.CityJSONRequest;
import app.bpartners.geojobs.repository.model.detection.Detection;
import app.bpartners.geojobs.service.CityJSONSafeModeProcessor;
import app.bpartners.geojobs.service.cityjson.CityJsonRoofMetrics;
import app.bpartners.geojobs.service.threed.exception.ThreedApiException;
import jakarta.persistence.EntityManager;
import java.io.File;
import java.util.*;
import java.util.function.Consumer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class FeatureRoofSlopeAndHeightRequestedService
    implements Consumer<FeatureRoofSlopeAndHeightRequested> {
  private static final String ROOF_SLOPE_DATA_STATUS_PROPERTY_NAME = "roof_slope_data_status";
  private static final String ROOF_HEIGHT_DATA_STATUS_PROPERTY_NAME = "roof_height_data_status";
  private final DetectionRepository detectionRepository;
  private final CityJSONSafeModeProcessor cityJSONSafeModeProcessor;
  private final BucketComponent bucketComponent;
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

    var delimitationFeaturesWithHeightAndSlopeProperties =
        computeHeightAndSlopeProperties(detection, delimitationFeatures);

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

  private List<Feature> computeHeightAndSlopeProperties(
      Detection detection, List<Feature> delimitationFeatures) {
    return delimitationFeatures.stream()
        .map(
            delimitation -> {
              var actualProperties = new HashMap<String, Object>();
              if (delimitation.getProperties() != null) {
                actualProperties.putAll(delimitation.getProperties());
              }
              var roofMetrics = computeRoofMetrics(detection, delimitation);

              var slopeValue = roofMetrics.map(CityJsonRoofMetrics::slopeInDegrees).orElse(0d);
              var heightValue = roofMetrics.map(CityJsonRoofMetrics::heightInMeters).orElse(0d);
              var lidarDataStatus = roofMetrics.isPresent() ? AVAILABLE : UNAVAILABLE;
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

  /**
   * Generates the roof CityJSON in safe mode (3DBag roofer, threed as fallback) and reads the slope
   * and height from it. Empty when there is no lidar data for the roof; any other failure is
   * rethrown so that the event is retried.
   */
  private Optional<CityJsonRoofMetrics> computeRoofMetrics(
      Detection detection, Feature delimitation) {
    var request =
        CityJSONRequest.builder()
            .id(randomUUID().toString())
            .communityOwnerId(detection.getCommunityOwnerId())
            .delimitations(List.of(toDomainFeature(delimitation)))
            .lidarProcessorType(SAFE_MODE)
            .build();
    List<File> cityJsonFiles = new ArrayList<>();
    try {
      var cityJsons = cityJSONSafeModeProcessor.generate(request).cityJsons();
      cityJsons.forEach(
          cityJson -> cityJsonFiles.add(bucketComponent.download(cityJson.getS3FileKey())));
      return CityJsonRoofMetrics.from(cityJsonFiles);
    } catch (ThreedApiException e) {
      if (e.isNoLidarDataFound()) {
        log.warn("No lidar data found for detection(id={}) roof", detection.getId(), e);
        return Optional.empty();
      }
      throw e;
    } finally {
      cityJsonFiles.forEach(File::delete);
    }
  }
}
