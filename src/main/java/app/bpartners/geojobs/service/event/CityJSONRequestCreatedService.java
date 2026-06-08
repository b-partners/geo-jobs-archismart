package app.bpartners.geojobs.service.event;

import static app.bpartners.geojobs.model.lidar.LidarProcessorType.DEFAULT;
import static app.bpartners.geojobs.model.lidar.planes.model.LasRoofDelimitationType.ROOF_SEGMENT_FACE_DELIMITATION;
import static app.bpartners.geojobs.repository.model.cityjson.CityJSONRequestStatus.*;
import static app.bpartners.geojobs.repository.model.cityjson.CityJSONRequestStep.*;
import static java.util.stream.Collectors.toSet;

import app.bpartners.geojobs.endpoint.event.EventProducer;
import app.bpartners.geojobs.endpoint.event.model.CityJSONRequestCreated;
import app.bpartners.geojobs.endpoint.event.model.ThreeDRequestMonitoringTriggered;
import app.bpartners.geojobs.endpoint.rest.controller.mapper.FeatureMapper;
import app.bpartners.geojobs.file.bucket.BucketComponent;
import app.bpartners.geojobs.model.lidar.planes.model.LasRoofDelimitationType;
import app.bpartners.geojobs.repository.CityJSONRequestRepository;
import app.bpartners.geojobs.repository.CommunityAuthorizationRepository;
import app.bpartners.geojobs.repository.model.Feature;
import app.bpartners.geojobs.repository.model.cityjson.*;
import app.bpartners.geojobs.service.CityJSON3DBagRooferProcessor;
import app.bpartners.geojobs.service.cityjson.LidarDataToCityJsonProcessor;
import app.bpartners.geojobs.service.cityjson.texture.CityJsonTextureComputer;
import app.bpartners.geojobs.service.lidar.LasRoofsPointsExtractor;
import app.bpartners.geojobs.service.lidar.PointsExtractionResult;
import jakarta.persistence.EntityManager;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.locationtech.jts.geom.Geometry;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class CityJSONRequestCreatedService implements Consumer<CityJSONRequestCreated> {
  private final CityJSONRequestRepository cityJSONRequestRepository;
  private final LasRoofsPointsExtractor lasRoofsPointsExtractor;
  private final LidarDataToCityJsonProcessor cityJsonProcessor;
  private final FeatureMapper featureMapper;
  private final EntityManager entityManager;
  private final BucketComponent bucketComponent;
  private final EventProducer eventProducer;
  private final CommunityAuthorizationRepository communityAuthorizationRepository;
  private final CityJSON3DBagRooferProcessor cityJson3DBagRooferProcessor;
  private final CityJsonTextureComputer textureComputer;

  @SneakyThrows
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

    processCityJSONFacade(request);
  }

  private void succeedCityJsonRequest(CityJSONRequest request, List<CityJSON> cityJsonList) {
    var updated =
        request.toBuilder()
            .status(FINISHED)
            .step(GEOMETRY_CONSTRUCTION)
            .cityJsons(cityJsonList)
            .build();
    entityManager.clear();
    cityJSONRequestRepository.save(updated);
  }

  private static boolean isUnavailable(PointsExtractionResult result) {
    if (result.data().isEmpty()) {
      return true;
    }
    return result.data().values().stream().anyMatch(data -> data.getPoints().isEmpty());
  }

  private CityJSON toCityJSON(CityJSONRequest request, PointsExtractionResult result) {
    var filename = String.format("%s.json", request.getId());
    var fileKey = String.format("city_jsons/%s", filename);
    log.info("PointsExtractionResult={}", result.data().values());
    var file = cityJsonProcessor.apply(filename, result);

    var texturedFile = textureComputer.applyTexture(request, file);

    bucketComponent.upload(texturedFile, fileKey);

    return CityJSON.builder().id(filename).request(request).s3FileKey(fileKey).build();
  }

  private Set<Geometry> toGeometries(List<Feature> delimitations) {
    return delimitations.stream()
        .map(featureMapper::domainToGeometryWithMultipolygonHandler)
        .collect(toSet());
  }

  private void updateStatus(
      CityJSONRequest request, CityJSONRequestStatus status, CityJSONRequestStep step) {
    var updatedStatus = request.toBuilder().status(status).step(step).build();
    entityManager.clear();
    cityJSONRequestRepository.save(updatedStatus);
  }

  private LasRoofDelimitationType getType(CityJSONRequest request) {
    return switch (request.getDelimitationObjectType()) {
      case BUILDING_ROOF_SEGMENT_FACE -> ROOF_SEGMENT_FACE_DELIMITATION;
      case null, default -> LasRoofDelimitationType.ENTIRE_ROOF_DELIMITATION;
    };
  }

  private void processCityJSONFacade(CityJSONRequest request) {
    var generationType = getType(request);
    if (ROOF_SEGMENT_FACE_DELIMITATION.equals(generationType)) {
      processByInternalMethod(request);
      return;
    }
    processFullAutomaticFacade(request);
  }

  private void processByInternalMethod(CityJSONRequest request) {
    try {
      var requestDelimitations = request.getRequestDelimitations();
      var pointsExtractionResult =
          lasRoofsPointsExtractor.apply(getType(request), toGeometries(requestDelimitations));

      if (isUnavailable(pointsExtractionResult)) {
        updateStatus(request, UNAVAILABLE, POINTS_CLOUD_PRE_PROCESSING);
        return;
      }

      var cityJson = toCityJSON(request, pointsExtractionResult);
      succeedCityJsonRequest(request, List.of(cityJson));
    } catch (Exception e) {
      log.error(e.getMessage());
      updateStatus(request, FAILED, GEOMETRY_CONSTRUCTION);
      throw e;
    }
  }

  private void processFullAutomaticFacade(CityJSONRequest request) {
    if (request.getLidarProcessorType() != null
        && request.getLidarProcessorType().equals(DEFAULT)) {
      processByInternalMethod(request);
    }
    processFullAutomaticBy3DBag(request);
  }

  private void processFullAutomaticBy3DBag(CityJSONRequest request) {
    try {
      var cityJsonFrom3dBag = cityJson3DBagRooferProcessor.apply(request);
      succeedCityJsonRequest(request, cityJsonFrom3dBag);
    } catch (Exception e) {
      log.error(
          "Unable to process Lidar for request {}, error {}", request.getId(), e.getMessage());
      updateStatus(request, FAILED, GEOMETRY_CONSTRUCTION);
    }
  }
}
