package app.bpartners.geojobs.service;

import static app.bpartners.geojobs.repository.model.cityjson.CityJSONRequestStatus.FAILED;
import static app.bpartners.geojobs.repository.model.cityjson.CityJSONRequestStatus.PROCESSING;
import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import app.bpartners.geojobs.endpoint.event.EventProducer;
import app.bpartners.geojobs.endpoint.event.model.CityJSONRequestCreated;
import app.bpartners.geojobs.endpoint.event.model.ThreeDMultipleAddressRequested;
import app.bpartners.geojobs.endpoint.rest.model.Geometry;
import app.bpartners.geojobs.endpoint.rest.model.Point;
import app.bpartners.geojobs.model.exception.ApiException;
import app.bpartners.geojobs.model.exception.BadRequestException;
import app.bpartners.geojobs.repository.CityJSONRequestRepository;
import app.bpartners.geojobs.repository.model.Feature;
import app.bpartners.geojobs.repository.model.cityjson.CityJSONRequest;
import app.bpartners.geojobs.repository.model.detection.FeatureWithDelimitation;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class CityJSONRequestServiceTest {
  CityJSONRequestRepository cityJSONRequestRepositoryMock = mock();
  EventProducer eventProducerMock = mock();
  FeatureAddressConverter featureAddressConverterMock = mock();

  CityJSONRequestService subject =
      new CityJSONRequestService(
          cityJSONRequestRepositoryMock, eventProducerMock, featureAddressConverterMock);

  @SneakyThrows
  @Test
  void
      compute_feature_delimitations_with_city_json_request_created_event_when_unique_point_feature_provided() {
    var communityOwnerId = randomUUID().toString();
    var requestId = randomUUID().toString();
    var longitude = 0.2492928974906442;
    var latitude = 46.65193378162583;
    var pointFeature = somePointFeature(longitude, latitude);
    Feature featureDelimitationConvertedMock = mock();
    when(cityJSONRequestRepositoryMock.findByIdAndCommunityOwnerId(requestId, communityOwnerId))
        .thenReturn(Optional.empty());
    when(featureAddressConverterMock.apply(null, longitude, latitude))
        .thenReturn(featureDelimitationConvertedMock);
    when(cityJSONRequestRepositoryMock.save(any()))
        .thenAnswer(invocationOnMock -> invocationOnMock.getArgument(0));
    var cityJSONRequest =
        CityJSONRequest.builder()
            .id(requestId)
            .communityOwnerId(communityOwnerId)
            .delimitationObjectType(null)
            .delimitations(List.of(pointFeature))
            .build();

    var actual = subject.process(cityJSONRequest);

    assertEquals(
        cityJSONRequest.toBuilder()
            .status(PROCESSING)
            .featuresWithDelimitation(
                List.of(
                    new FeatureWithDelimitation(
                        pointFeature, List.of(featureDelimitationConvertedMock))))
            .build(),
        actual);
    var listCaptor = ArgumentCaptor.forClass(List.class);
    verify(eventProducerMock, times(1)).accept(listCaptor.capture());
    var cityJSONRequestCreated = (CityJSONRequestCreated) listCaptor.getValue().getFirst();
    assertEquals(
        CityJSONRequestCreated.builder()
            .requestId(requestId)
            .communityOwnerId(communityOwnerId)
            .build(),
        cityJSONRequestCreated);
  }

  @SneakyThrows
  @Test
  void produces_exception_when_unique_point_feature_provided_and_unable_to_convert_point() {
    var communityOwnerId = randomUUID().toString();
    var requestId = randomUUID().toString();
    var longitude = 0.2492928974906442;
    var latitude = 46.65193378162583;
    var pointFeature = somePointFeature(longitude, latitude);
    when(cityJSONRequestRepositoryMock.findByIdAndCommunityOwnerId(requestId, communityOwnerId))
        .thenReturn(Optional.empty());
    when(featureAddressConverterMock.apply(null, longitude, latitude))
        .thenThrow(ApiException.class);
    when(cityJSONRequestRepositoryMock.save(any()))
        .thenAnswer(invocationOnMock -> invocationOnMock.getArgument(0));
    var cityJSONRequest =
        CityJSONRequest.builder()
            .id(requestId)
            .communityOwnerId(communityOwnerId)
            .delimitationObjectType(null)
            .delimitations(List.of(pointFeature))
            .build();

    var actual = subject.process(cityJSONRequest);

    verify(eventProducerMock, never()).accept(any());
    assertEquals(
        cityJSONRequest.toBuilder().status(FAILED).featuresWithDelimitation(null).build(), actual);
  }

  @SneakyThrows
  @Test
  void
      compute_feature_delimitations_with_multiple_address_requested_event_when_multiple_point_feature_provided() {
    var communityOwnerId = randomUUID().toString();
    var requestId = randomUUID().toString();
    var longitudeOne = 0.2492928974906442;
    var latitudeOne = 46.65193378162583;
    var longitudeTwo = 0.2492928974906448;
    var latitudeTwo = 46.65193378162582;
    var pointFeatureOne = somePointFeature(longitudeOne, latitudeOne);
    var pointFeatureTwo = somePointFeature(longitudeTwo, latitudeTwo);
    Feature featureDelimitationConvertedMockOne = mock();
    Feature featureDelimitationConvertedMockTwo = mock();
    when(cityJSONRequestRepositoryMock.findByIdAndCommunityOwnerId(requestId, communityOwnerId))
        .thenReturn(Optional.empty());
    when(featureAddressConverterMock.apply(null, longitudeOne, latitudeOne))
        .thenReturn(featureDelimitationConvertedMockOne);
    when(featureAddressConverterMock.apply(null, longitudeTwo, latitudeTwo))
        .thenReturn(featureDelimitationConvertedMockTwo);
    when(cityJSONRequestRepositoryMock.save(any()))
        .thenAnswer(invocationOnMock -> invocationOnMock.getArgument(0));
    var cityJSONRequest =
        CityJSONRequest.builder()
            .id(requestId)
            .communityOwnerId(communityOwnerId)
            .delimitationObjectType(null)
            .delimitations(List.of(pointFeatureOne, pointFeatureTwo))
            .build();

    var actual = subject.process(cityJSONRequest);

    assertEquals(
        cityJSONRequest.toBuilder().status(PROCESSING).featuresWithDelimitation(null).build(),
        actual);
    var listCaptor = ArgumentCaptor.forClass(List.class);
    verify(eventProducerMock, times(1)).accept(listCaptor.capture());
    var cityJSONRequestCreated = (ThreeDMultipleAddressRequested) listCaptor.getValue().getFirst();
    assertEquals(
        ThreeDMultipleAddressRequested.builder()
            .requestIdentifier(requestId)
            .communityOwnerId(communityOwnerId)
            .points(
                List.of(
                    new Point()
                        .type(null)
                        .coordinates(
                            List.of(
                                BigDecimal.valueOf(longitudeOne), BigDecimal.valueOf(latitudeOne))),
                    new Point()
                        .type(null)
                        .coordinates(
                            List.of(
                                BigDecimal.valueOf(longitudeTwo),
                                BigDecimal.valueOf(latitudeTwo)))))
            .build(),
        cityJSONRequestCreated);
  }

  private static Feature somePointFeature(double longitude, double latitude)
      throws JsonProcessingException {
    return Feature.builder()
        .properties(new HashMap<>())
        .geometry(
            Feature.FeatureGeometry.builder()
                .actualInstanceStringValue(
                    new ObjectMapper()
                        .writeValueAsString(
                            new Point()
                                .coordinates(
                                    List.of(
                                        BigDecimal.valueOf(longitude),
                                        BigDecimal.valueOf(latitude)))))
                .geometryType(Geometry.TypeEnum.POINT)
                .build())
        .build();
  }

  @Test
  void throw_exception_when_already_processed_request() {
    var requestIdentifier = randomUUID().toString();
    var communityOwnerId = randomUUID().toString();
    when(cityJSONRequestRepositoryMock.findByIdAndCommunityOwnerId(
            requestIdentifier, communityOwnerId))
        .thenReturn(Optional.of(mock()));

    var actual =
        assertThrows(
            BadRequestException.class,
            () -> subject.processAddressRequest(requestIdentifier, List.of(), communityOwnerId));

    assertEquals(
        "Process request with id "
            + requestIdentifier
            + " can not be either updated or processed again",
        actual.getMessage());
  }

  @Test
  void produces_event_when_not_existing_request() {
    var requestIdentifier = randomUUID().toString();
    var communityOwnerId = randomUUID().toString();
    var someAddress = List.of("some address");
    when(cityJSONRequestRepositoryMock.findByIdAndCommunityOwnerId(
            requestIdentifier, communityOwnerId))
        .thenReturn(Optional.empty());
    when(cityJSONRequestRepositoryMock.save(any()))
        .thenAnswer(invocationOnMock -> invocationOnMock.getArgument(0));

    var actual = subject.processAddressRequest(requestIdentifier, someAddress, communityOwnerId);

    assertEquals(
        CityJSONRequest.builder()
            .id(requestIdentifier)
            .status(PROCESSING)
            .cityJsons(null)
            .delimitations(null)
            .communityOwnerId(communityOwnerId)
            .creationDatetime(actual.getCreationDatetime())
            .build(),
        actual);
    var listCaptor = ArgumentCaptor.forClass(List.class);
    verify(eventProducerMock, times(1)).accept(listCaptor.capture());
    ThreeDMultipleAddressRequested multipleAddressRequested =
        (ThreeDMultipleAddressRequested) listCaptor.getValue().getFirst();
    assertEquals(
        new ThreeDMultipleAddressRequested(requestIdentifier, communityOwnerId, someAddress, null),
        multipleAddressRequested);
  }
}
