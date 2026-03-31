package app.bpartners.geojobs.service.event;

import static app.bpartners.geojobs.repository.model.cityjson.CityJSONRequestStatus.*;
import static app.bpartners.geojobs.repository.model.cityjson.CityJSONRequestStep.*;
import static java.util.stream.Collectors.toSet;

import app.bpartners.geojobs.endpoint.event.EventProducer;
import app.bpartners.geojobs.endpoint.event.model.CityJSONRequestCreated;
import app.bpartners.geojobs.endpoint.event.model.ThreeDRequestMonitoringTriggered;
import app.bpartners.geojobs.endpoint.rest.controller.mapper.FeatureMapper;
import app.bpartners.geojobs.file.bucket.BucketComponent;
import app.bpartners.geojobs.repository.CityJSONRequestRepository;
import app.bpartners.geojobs.repository.CommunityAuthorizationRepository;
import app.bpartners.geojobs.repository.model.Feature;
import app.bpartners.geojobs.repository.model.cityjson.CityJSON;
import app.bpartners.geojobs.repository.model.cityjson.CityJSONRequest;
import app.bpartners.geojobs.repository.model.cityjson.CityJSONRequestStatus;
import app.bpartners.geojobs.repository.model.cityjson.CityJSONRequestStep;
import app.bpartners.geojobs.repository.model.detection.FeatureWithDelimitation;
import app.bpartners.geojobs.service.cityjson.LidarDataToCityJsonProcessor;
import app.bpartners.geojobs.service.lidar.LidarRoofsAnalysisProcessor;
import app.bpartners.geojobs.service.lidar.LidarRoofsAnalysisProcessor.RoofsAnalysisResult;
import app.bpartners.geojobs.service.lidar.model.LidarDataStatus;
import app.bpartners.geojobs.service.lidar.model.geometry.GeometryWithProperties;
import jakarta.persistence.EntityManager;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.locationtech.jts.geom.Geometry;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class CityJSONRequestCreatedService implements Consumer<CityJSONRequestCreated> {
  private final CityJSONRequestRepository cityJSONRequestRepository;
  private final LidarRoofsAnalysisProcessor lidarProcessor;
  private final LidarDataToCityJsonProcessor cityJsonProcessor;
  private final FeatureMapper featureMapper;
  private final EntityManager entityManager;
  private final BucketComponent bucketComponent;
  private final EventProducer eventProducer;
  private final CommunityAuthorizationRepository communityAuthorizationRepository;

  @Override
  public void accept(CityJSONRequestCreated created) {
    var communityAuthorization =
        communityAuthorizationRepository.findById(created.getCommunityOwnerId()).orElseThrow();
    if (!communityAuthorization.isIntegrationTestUsage()) {
      eventProducer.accept(
          List.of(
              new ThreeDRequestMonitoringTriggered(
                  created.getRequestId(), created.getCommunityOwnerId())));
    }

    var request =
        cityJSONRequestRepository
            .findByIdAndCommunityOwnerId(created.getRequestId(), created.getCommunityOwnerId())
            .orElseThrow();

    try {
      var requestDelimitations = getRequestDelimitations(request);
      var lidarAnalysisResult =
          lidarProcessor.apply(toGeometryWithProperties(requestDelimitations));

      if (isError(lidarAnalysisResult)) {
        log.error("All data from lidar analysis was failed");
        updateStatus(request, FAILED, POINTS_CLOUD_PRE_PROCESSING);
        throw new LidarAllDataFailedException("All data from lidar analysis was failed");
      }

      if (isUnavailable(lidarAnalysisResult)) {
        updateStatus(request, UNAVAILABLE, POINTS_CLOUD_PRE_PROCESSING);
        return;
      }

      var cityJson = toCityJSON(request, lidarAnalysisResult);

      var updated =
          request.toBuilder()
              .status(FINISHED)
              .step(GEOMETRY_CONSTRUCTION)
              .cityJsons(List.of(cityJson))
              .build();
      entityManager.clear();
      cityJSONRequestRepository.save(updated);
    } catch (LidarAllDataFailedException e) {
      throw e;
    } catch (Exception e) {
      log.error(e.getMessage());
      updateStatus(request, FAILED, GEOMETRY_CONSTRUCTION);
      throw e;
    }
  }

  private List<Feature> getRequestDelimitations(CityJSONRequest request) {
    if (request.getFeaturesWithDelimitation() != null
        && !request.getFeaturesWithDelimitation().isEmpty()) {
      return request.getFeaturesWithDelimitation().stream()
          .map(FeatureWithDelimitation::delimitations)
          .flatMap(List::stream)
          .toList();
    }
    return request.getDelimitations();
  }

  private static boolean isUnavailable(RoofsAnalysisResult roofsAnalysisResult) {
    return roofsAnalysisResult.roofsData().values().stream()
        .allMatch(data -> LidarDataStatus.UNAVAILABLE.equals(data.status()));
  }

  private static boolean isError(RoofsAnalysisResult roofsAnalysisResult) {
    return roofsAnalysisResult.roofsData().values().stream()
        .allMatch(data -> LidarDataStatus.EXTRACTION_ERROR.equals(data.status()));
  }

  private CityJSON toCityJSON(CityJSONRequest request, RoofsAnalysisResult lidarAnalysisResult) {
    var filename = String.format("%s.json", request.getId());
    var fileKey = String.format("city_jsons/%s", filename);
    var file = cityJsonProcessor.apply(filename, lidarAnalysisResult);

    bucketComponent.upload(file, fileKey);

    return CityJSON.builder().id(filename).request(request).s3FileKey(fileKey).build();
  }

  private Set<GeometryWithProperties> toGeometryWithProperties(List<Feature> delimitations) {
    return delimitations.stream()
        .map(
            delimitation -> {
              Geometry geometry = featureMapper.domainToGeometry(delimitation);
              Map<String, Object> properties =
                  delimitation.getProperties() == null
                      ? new HashMap<>()
                      : delimitation.getProperties();
              return new GeometryWithProperties(geometry, properties);
            })
        .collect(toSet());
  }

  private void updateStatus(
      CityJSONRequest request, CityJSONRequestStatus status, CityJSONRequestStep step) {
    var updatedStatus = request.toBuilder().status(status).step(step).build();
    entityManager.clear();
    cityJSONRequestRepository.save(updatedStatus);
  }

  public static class LidarAllDataFailedException extends RuntimeException {
    public LidarAllDataFailedException(String message) {
      super(message);
    }
  }
}
