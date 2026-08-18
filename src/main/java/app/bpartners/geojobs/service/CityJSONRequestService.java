package app.bpartners.geojobs.service;

import static app.bpartners.geojobs.endpoint.rest.controller.v1.mapper.FeatureMapper.toDomainFeature;
import static app.bpartners.geojobs.endpoint.rest.model.DelimitationType.USER_DEFINED_DELIMITATION;
import static app.bpartners.geojobs.repository.model.cityjson.CityJSONDelimitationObjectType.BUILDING_ROOF;
import static app.bpartners.geojobs.repository.model.cityjson.CityJSONRequestStatus.FAILED;
import static app.bpartners.geojobs.repository.model.cityjson.CityJSONRequestStatus.PROCESSING;
import static app.bpartners.geojobs.repository.model.cityjson.CityJSONRequestStep.POINTS_CLOUD_PRE_PROCESSING;
import static app.bpartners.geojobs.repository.model.cityjson.CityJSONRequestStep.REQUEST_ACCEPTED;

import app.bpartners.geojobs.endpoint.event.EventProducer;
import app.bpartners.geojobs.endpoint.event.model.CityJSONRequestCreated;
import app.bpartners.geojobs.endpoint.event.model.ThreeDMultipleAddressRequested;
import app.bpartners.geojobs.endpoint.rest.model.Feature;
import app.bpartners.geojobs.endpoint.rest.model.Point;
import app.bpartners.geojobs.model.exception.ApiException;
import app.bpartners.geojobs.model.exception.BadRequestException;
import app.bpartners.geojobs.model.exception.NotFoundException;
import app.bpartners.geojobs.model.lidar.LidarProcessorType;
import app.bpartners.geojobs.repository.CityJSONRequestRepository;
import app.bpartners.geojobs.repository.model.cityjson.CityJSONRequest;
import app.bpartners.geojobs.repository.model.detection.FeatureWithDelimitation;
import app.bpartners.geojobs.service.event.CityJSONRequestCreatedService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
@SuppressWarnings({"rawtypes", "unchecked"})
public class CityJSONRequestService {
  private final CityJSONRequestRepository cityJSONRequestRepository;
  private final EventProducer eventProducer;
  private final FeatureAddressConverter featureAddressConverter;
  private final CityJSONRequestCreatedService requestCreatedService;

  public CityJSONRequest processAddressRequest(
      String requestIdentifier,
      List<String> addresses,
      String communityOwnerId,
      LidarProcessorType lidarProcessorType) {
    var optionalRequest =
        cityJSONRequestRepository.findByIdAndCommunityOwnerId(requestIdentifier, communityOwnerId);
    if (optionalRequest.isPresent()) {
      throw new BadRequestException(
          "Process request with id "
              + requestIdentifier
              + " can not be either updated or processed again");
    }
    var savedRequest =
        cityJSONRequestRepository.save(
            CityJSONRequest.builder()
                .id(requestIdentifier)
                .status(PROCESSING)
                .communityOwnerId(communityOwnerId)
                .lidarProcessorType(lidarProcessorType)
                .build());
    eventProducer.accept(
        List.of(
            new ThreeDMultipleAddressRequested(
                savedRequest.getId(), communityOwnerId, addresses, null, lidarProcessorType)));
    return savedRequest;
  }

  public CityJSONRequest processSync(CityJSONRequest cityJSONRequest) {
    var created = create(cityJSONRequest);
    if (FAILED.equals(created.getStatus())) return created;

    var requestIdentifier = created.getId();
    requestCreatedService.accept(
        CityJSONRequestCreated.builder()
            .requestId(requestIdentifier)
            .communityOwnerId(cityJSONRequest.getCommunityOwnerId())
            .lidarProcessorType(cityJSONRequest.getLidarProcessorType())
            .build(),
        true);

    return getByIdAndCommunityOwnerId(requestIdentifier, cityJSONRequest.getCommunityOwnerId());
  }

  public CityJSONRequest process(CityJSONRequest cityJSONRequest) {
    var requestIdentifier = cityJSONRequest.getId();

    var created = create(cityJSONRequest);
    if (FAILED.equals(created.getStatus())) return created;

    var pointFeatureList = getPointFeatures(cityJSONRequest);
    var cityJSONRequestBuilder = created.toBuilder();

    if (pointFeatureList.size() > 1) {
      var pointCorrespondingToAddresses =
          pointFeatureList.stream().map(feature -> feature.getGeometry().getPoint()).toList();
      cityJSONRequestBuilder.step(REQUEST_ACCEPTED);
      eventProducer.accept(
          List.of(
              new ThreeDMultipleAddressRequested(
                  requestIdentifier,
                  cityJSONRequest.getCommunityOwnerId(),
                  null,
                  pointCorrespondingToAddresses,
                  cityJSONRequest.getLidarProcessorType())));
    } else {
      cityJSONRequestBuilder.step(POINTS_CLOUD_PRE_PROCESSING);
      eventProducer.accept(
          List.of(
              CityJSONRequestCreated.builder()
                  .requestId(requestIdentifier)
                  .communityOwnerId(cityJSONRequest.getCommunityOwnerId())
                  .lidarProcessorType(cityJSONRequest.getLidarProcessorType())
                  .build()));
    }

    return cityJSONRequestRepository.save(cityJSONRequestBuilder.status(PROCESSING).build());
  }

  public CityJSONRequest oldProcess(CityJSONRequest cityJSONRequest) {
    var requestIdentifier = cityJSONRequest.getId();
    var optionalRequest =
        cityJSONRequestRepository.findByIdAndCommunityOwnerId(
            requestIdentifier, cityJSONRequest.getCommunityOwnerId());

    if (optionalRequest.isPresent()) {
      return optionalRequest.get();
    }

    var cityJSONRequestBuilder = cityJSONRequest.toBuilder();
    try {
      addDelimitationIfOnePointFeatureIsPresent(cityJSONRequest, cityJSONRequestBuilder);
    } catch (ApiException e) {
      logApiError(e);
      return cityJSONRequestRepository.save(cityJSONRequest.toBuilder().status(FAILED).build());
    }

    var saved = cityJSONRequestRepository.save(cityJSONRequestBuilder.status(PROCESSING).build());

    var pointFeatureList = getPointFeatures(cityJSONRequest);
    if (pointFeatureList.size() > 1) {
      var pointCorrespondingToAddresses =
          pointFeatureList.stream().map(feature -> feature.getGeometry().getPoint()).toList();
      eventProducer.accept(
          List.of(
              new ThreeDMultipleAddressRequested(
                  requestIdentifier,
                  cityJSONRequest.getCommunityOwnerId(),
                  null,
                  pointCorrespondingToAddresses,
                  null)));
    } else {
      eventProducer.accept(
          List.of(
              CityJSONRequestCreated.builder()
                  .requestId(saved.getId())
                  .communityOwnerId(cityJSONRequest.getCommunityOwnerId())
                  .build()));
    }

    return saved;
  }

  public CityJSONRequest getByIdAndCommunityOwnerId(String requestId, String communityOwnerId) {
    var optionalRequest =
        cityJSONRequestRepository.findByIdAndCommunityOwnerId(requestId, communityOwnerId);
    if (optionalRequest.isEmpty()) {
      throw new NotFoundException(String.format("CityJSONRequest.id=%s was not found", requestId));
    }

    return optionalRequest.get();
  }

  private CityJSONRequest create(CityJSONRequest cityJSONRequest) {
    var requestIdentifier = cityJSONRequest.getId();
    var optionalRequest =
        cityJSONRequestRepository.findByIdAndCommunityOwnerId(
            requestIdentifier, cityJSONRequest.getCommunityOwnerId());

    if (optionalRequest.isPresent()) {
      throw new BadRequestException(
          "Process request with id "
              + requestIdentifier
              + " can not be either updated or processed again");
    }

    var cityJSONRequestBuilder = cityJSONRequest.toBuilder();
    if (USER_DEFINED_DELIMITATION.equals(cityJSONRequest.getDelimitationType())) {
      cityJSONRequestBuilder.featuresWithDelimitation(
          cityJSONRequest.getDelimitations().stream()
              .map(feature -> new FeatureWithDelimitation(feature, List.of(feature)))
              .toList());
    } else {
      try {
        addDelimitationIfOnePointFeatureIsPresent(cityJSONRequest, cityJSONRequestBuilder);
      } catch (ApiException e) {
        logApiError(e);
        return cityJSONRequestRepository.save(
            cityJSONRequest.toBuilder().status(FAILED).step(REQUEST_ACCEPTED).build());
      }
    }

    return cityJSONRequestRepository.save(
        cityJSONRequestBuilder.status(PROCESSING).step(REQUEST_ACCEPTED).build());
  }

  private void addDelimitationIfOnePointFeatureIsPresent(
      CityJSONRequest request, CityJSONRequest.CityJSONRequestBuilder cityJSONRequestBuilder) {
    var pointFeatureList = getPointFeatures(request);
    if (pointFeatureList.size() != 1) {
      return;
    }
    var delimitationObjectType = request.getDelimitationObjectType();
    if (delimitationObjectType != null && !BUILDING_ROOF.equals(delimitationObjectType)) {
      return;
    }

    var featureWithDelimitations =
        pointFeatureList.stream()
            .map(
                feature -> {
                  var point = feature.getGeometry().getPoint();
                  var longitude = point.getCoordinates().getFirst();
                  var latitude = point.getCoordinates().getLast();
                  return new FeatureWithDelimitation(
                      toDomainFeature(feature),
                      List.of(
                          featureAddressConverter.apply(
                              null, longitude.doubleValue(), latitude.doubleValue())));
                })
            .toList();
    cityJSONRequestBuilder.featuresWithDelimitation(featureWithDelimitations);
  }

  private static List<Feature> getPointFeatures(CityJSONRequest request) {
    return request.getRestFeatureDelimitations().stream()
        .filter(
            feature ->
                feature.getGeometry() != null
                    && feature.getGeometry().getActualInstance() instanceof Point)
        .toList();
  }

  private static void logApiError(ApiException e) {
    log.error(
        "Conversion of addresses to features failed with API exception from dashboard {}",
        e.getMessage());
  }
}
