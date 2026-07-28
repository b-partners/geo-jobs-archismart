package app.bpartners.geojobs.service.event;

import static app.bpartners.geojobs.endpoint.rest.model.Geometry.TypeEnum.POINT;
import static app.bpartners.geojobs.model.DelimitationObjectType.BUILDING;
import static app.bpartners.geojobs.repository.model.ArcgisImageZoom.HOUSES_0;
import static app.bpartners.geojobs.repository.model.cityjson.CityJSONRequestStatus.FAILED;
import static app.bpartners.geojobs.repository.model.cityjson.CityJSONRequestStep.POINTS_CLOUD_PRE_PROCESSING;
import static app.bpartners.geojobs.repository.model.cityjson.CityJSONRequestStep.REQUEST_ACCEPTED;

import app.bpartners.geojobs.endpoint.event.EventProducer;
import app.bpartners.geojobs.endpoint.event.model.CityJSONRequestCreated;
import app.bpartners.geojobs.endpoint.event.model.ThreeDMultipleAddressRequested;
import app.bpartners.geojobs.endpoint.rest.model.Point;
import app.bpartners.geojobs.model.exception.ApiException;
import app.bpartners.geojobs.model.lidar.LidarProcessorType;
import app.bpartners.geojobs.repository.CityJSONRequestRepository;
import app.bpartners.geojobs.repository.model.Feature;
import app.bpartners.geojobs.repository.model.cityjson.CityJSONRequest;
import app.bpartners.geojobs.repository.model.detection.FeatureWithDelimitation;
import app.bpartners.geojobs.service.FeatureAddressConverter;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.function.Consumer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ThreeDMultipleAddressRequestedService
    implements Consumer<ThreeDMultipleAddressRequested> {
  private final FeatureAddressConverter featureAddressConverter;
  private final CityJSONRequestRepository cityJSONRequestRepository;
  private final EventProducer eventProducer;

  @Override
  public void accept(ThreeDMultipleAddressRequested requestEvent) {
    var requestIdentifier = requestEvent.getRequestIdentifier();
    var communityOwnerId = requestEvent.getCommunityOwnerId();
    var persistedRequest =
        cityJSONRequestRepository
            .findByIdAndCommunityOwnerId(requestIdentifier, communityOwnerId)
            .orElseThrow();
    var lidarProcessorType = persistedRequest.getLidarProcessorType();
    var addresses = requestEvent.getAddresses();
    var points = requestEvent.getPoints();

    handleAddresses(addresses, persistedRequest, lidarProcessorType);
    handlePoints(points, persistedRequest, lidarProcessorType);
  }

  private void handleAddresses(
      List<String> addresses,
      CityJSONRequest persistedRequest,
      LidarProcessorType lidarProcessorType) {
    if (addresses == null) {
      return;
    }
    var convertedFeatures = new ArrayList<Feature>();
    var skippedAddresses = new ArrayList<String>();
    for (var addressValue : addresses) {
      try {
        convertedFeatures.add(featureAddressConverter.apply(addressValue, BUILDING));
      } catch (ApiException e) {
        log.warn(
            "Skipping address={} of CityJSONRequest(id={}) as its conversion to feature failed",
            addressValue,
            persistedRequest.getId(),
            e);
        skippedAddresses.add(addressValue);
      }
    }
    if (convertedFeatures.isEmpty() && !skippedAddresses.isEmpty()) {
      log.error(
          "Conversion of all {} address(es) of CityJSONRequest(id={}) failed",
          addresses.size(),
          persistedRequest.getId());
      cityJSONRequestRepository.save(
          persistedRequest.toBuilder().step(REQUEST_ACCEPTED).status(FAILED).build());
      return;
    }
    if (!skippedAddresses.isEmpty()) {
      log.warn(
          "Processing CityJSONRequest(id={}) with {} skipped address(es) out of {} : {}",
          persistedRequest.getId(),
          skippedAddresses.size(),
          addresses.size(),
          skippedAddresses);
    }

    var saved =
        cityJSONRequestRepository.save(
            persistedRequest.toBuilder()
                .delimitations(convertedFeatures)
                .step(POINTS_CLOUD_PRE_PROCESSING)
                .build());

    eventProducer.accept(
        List.of(
            CityJSONRequestCreated.builder()
                .requestId(saved.getId())
                .communityOwnerId(persistedRequest.getCommunityOwnerId())
                .lidarProcessorType(lidarProcessorType)
                .build()));
  }

  private void handlePoints(
      List<Point> points, CityJSONRequest persistedRequest, LidarProcessorType lidarProcessorType) {
    if (points == null) {
      return;
    }
    var convertedFeatureDelimitations = new ArrayList<FeatureWithDelimitation>();
    var skippedPoints = new ArrayList<Point>();
    for (var point : points) {
      var longitude = point.getCoordinates().getFirst().doubleValue();
      var latitude = point.getCoordinates().getLast().doubleValue();
      Feature pointFeature;
      try {
        pointFeature =
            new Feature(
                null,
                HOUSES_0.getZoomLevel(),
                new HashMap<>(),
                new Feature.FeatureGeometry(POINT, new ObjectMapper().writeValueAsString(point)));
      } catch (JsonProcessingException e) {
        throw new RuntimeException(e);
      }
      try {
        var retrievedObjectTypeDelimitations =
            List.of(featureAddressConverter.apply(null, longitude, latitude));
        convertedFeatureDelimitations.add(
            new FeatureWithDelimitation(pointFeature, retrievedObjectTypeDelimitations));
      } catch (ApiException e) {
        log.warn(
            "Skipping point (longitude={}, latitude={}) of CityJSONRequest(id={}) as its"
                + " conversion to feature failed",
            longitude,
            latitude,
            persistedRequest.getId(),
            e);
        skippedPoints.add(point);
      }
    }
    if (convertedFeatureDelimitations.isEmpty() && !skippedPoints.isEmpty()) {
      log.error(
          "Conversion of all {} point(s) of CityJSONRequest(id={}) failed",
          points.size(),
          persistedRequest.getId());
      cityJSONRequestRepository.save(persistedRequest.toBuilder().status(FAILED).build());
      return;
    }
    if (!skippedPoints.isEmpty()) {
      log.warn(
          "Processing CityJSONRequest(id={}) with {} skipped point(s) out of {} : {}",
          persistedRequest.getId(),
          skippedPoints.size(),
          points.size(),
          skippedPoints);
    }

    var saved =
        cityJSONRequestRepository.save(
            persistedRequest.toBuilder()
                .featuresWithDelimitation(convertedFeatureDelimitations)
                .build());

    eventProducer.accept(
        List.of(
            CityJSONRequestCreated.builder()
                .requestId(saved.getId())
                .communityOwnerId(persistedRequest.getCommunityOwnerId())
                .lidarProcessorType(lidarProcessorType)
                .build()));
  }
}
