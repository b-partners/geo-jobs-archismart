package app.bpartners.geojobs.service.event;

import static app.bpartners.geojobs.endpoint.rest.model.Geometry.TypeEnum.POINT;
import static app.bpartners.geojobs.model.DelimitationObjectType.BUILDING;
import static app.bpartners.geojobs.repository.model.ArcgisImageZoom.HOUSES_0;
import static app.bpartners.geojobs.repository.model.cityjson.CityJSONRequestStatus.FAILED;

import app.bpartners.geojobs.endpoint.event.EventProducer;
import app.bpartners.geojobs.endpoint.event.model.CityJSONRequestCreated;
import app.bpartners.geojobs.endpoint.event.model.ThreeDMultipleAddressRequested;
import app.bpartners.geojobs.endpoint.rest.model.Point;
import app.bpartners.geojobs.model.exception.ApiException;
import app.bpartners.geojobs.repository.CityJSONRequestRepository;
import app.bpartners.geojobs.repository.model.Feature;
import app.bpartners.geojobs.repository.model.cityjson.CityJSONRequest;
import app.bpartners.geojobs.repository.model.detection.FeatureWithDelimitation;
import app.bpartners.geojobs.service.FeatureAddressConverter;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
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
    var addresses = requestEvent.getAddresses();
    var points = requestEvent.getPoints();

    handleAddresses(addresses, persistedRequest);
    handlePoints(points, persistedRequest);
  }

  private void handleAddresses(List<String> addresses, CityJSONRequest persistedRequest) {
    if (addresses == null) {
      return;
    }
    List<Feature> convertedFeatures;
    try {
      convertedFeatures =
          addresses.stream()
              .map(addressValue -> featureAddressConverter.apply(addressValue, BUILDING))
              .toList();
    } catch (ApiException e) {
      log.error(
          "Conversion of addresses to features failed with API exception from dashboard {}",
          e.getMessage());
      cityJSONRequestRepository.save(persistedRequest.toBuilder().status(FAILED).build());
      return;
    }

    var saved =
        cityJSONRequestRepository.save(
            persistedRequest.toBuilder().delimitations(convertedFeatures).build());

    eventProducer.accept(
        List.of(
            CityJSONRequestCreated.builder()
                .requestId(saved.getId())
                .communityOwnerId(persistedRequest.getCommunityOwnerId())
                .build()));
  }

  private void handlePoints(List<Point> points, CityJSONRequest persistedRequest) {
    if (points == null) {
      return;
    }
    List<FeatureWithDelimitation> convertedFeatureDelimitations;
    try {
      convertedFeatureDelimitations =
          points.stream()
              .map(
                  point -> {
                    var longitude = point.getCoordinates().getFirst().doubleValue();
                    var latitude = point.getCoordinates().getLast().doubleValue();
                    Feature pointFeature;
                    try {
                      pointFeature =
                          new Feature(
                              null,
                              HOUSES_0.getZoomLevel(),
                              new HashMap<>(),
                              new Feature.FeatureGeometry(
                                  POINT, new ObjectMapper().writeValueAsString(point)));
                    } catch (JsonProcessingException e) {
                      throw new RuntimeException(e);
                    }
                    var retrievedObjectTypeDelimitations =
                        List.of(featureAddressConverter.apply(null, longitude, latitude));
                    return new FeatureWithDelimitation(
                        pointFeature, retrievedObjectTypeDelimitations);
                  })
              .toList();
    } catch (ApiException e) {
      log.error(
          "Conversion of addresses to features failed with API exception from dashboard {}",
          e.getMessage());
      cityJSONRequestRepository.save(persistedRequest.toBuilder().status(FAILED).build());
      return;
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
                .build()));
  }
}
