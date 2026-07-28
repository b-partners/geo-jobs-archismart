package app.bpartners.geojobs.service.event;

import static app.bpartners.geojobs.endpoint.rest.model.Geometry.TypeEnum.POINT;
import static app.bpartners.geojobs.model.DelimitationObjectType.BUILDING;
import static app.bpartners.geojobs.model.exception.ApiException.ExceptionType.SERVER_EXCEPTION;
import static app.bpartners.geojobs.repository.model.ArcgisImageZoom.HOUSES_0;
import static app.bpartners.geojobs.repository.model.cityjson.CityJSONRequestStatus.FAILED;
import static app.bpartners.geojobs.repository.model.cityjson.CityJSONRequestStep.POINTS_CLOUD_PRE_PROCESSING;
import static app.bpartners.geojobs.repository.model.cityjson.CityJSONRequestStep.REQUEST_ACCEPTED;
import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import app.bpartners.geojobs.endpoint.event.EventProducer;
import app.bpartners.geojobs.endpoint.event.model.CityJSONRequestCreated;
import app.bpartners.geojobs.endpoint.event.model.ThreeDMultipleAddressRequested;
import app.bpartners.geojobs.endpoint.rest.model.Point;
import app.bpartners.geojobs.model.exception.ApiException;
import app.bpartners.geojobs.model.exception.BadRequestException;
import app.bpartners.geojobs.repository.CityJSONRequestRepository;
import app.bpartners.geojobs.repository.model.Feature;
import app.bpartners.geojobs.repository.model.cityjson.CityJSONRequest;
import app.bpartners.geojobs.repository.model.detection.FeatureWithDelimitation;
import app.bpartners.geojobs.service.FeatureAddressConverter;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class ThreeDMultipleAddressRequestedServiceTest {
  FeatureAddressConverter featureAddressConverterMock = mock();
  CityJSONRequestRepository cityJSONRequestRepositoryMock = mock();
  EventProducer eventProducerMock = mock();
  ThreeDMultipleAddressRequestedService subject =
      new ThreeDMultipleAddressRequestedService(
          featureAddressConverterMock, cityJSONRequestRepositoryMock, eventProducerMock);

  @Test
  void do_not_produces_event_computing_when_address_conversion_fails() {
    var requestIdentifier = randomUUID().toString();
    var communityOwnerId = randomUUID().toString();
    var someAddress = "random " + randomUUID();
    var request = CityJSONRequest.builder().build();
    when(cityJSONRequestRepositoryMock.findByIdAndCommunityOwnerId(
            requestIdentifier, communityOwnerId))
        .thenReturn(Optional.of(request));
    when(featureAddressConverterMock.apply(someAddress, BUILDING))
        .thenThrow(new ApiException(SERVER_EXCEPTION, "Unable to convert address to feature"));
    when(cityJSONRequestRepositoryMock.save(any()))
        .thenAnswer(invocationOnMock -> invocationOnMock.getArgument(0));

    assertDoesNotThrow(
        () ->
            subject.accept(
                ThreeDMultipleAddressRequested.builder()
                    .requestIdentifier(requestIdentifier)
                    .communityOwnerId(communityOwnerId)
                    .addresses(List.of(someAddress))
                    .build()));
    var savedFailedRequestCaptor = ArgumentCaptor.forClass(CityJSONRequest.class);
    verify(cityJSONRequestRepositoryMock, times(1)).save(savedFailedRequestCaptor.capture());
    verify(featureAddressConverterMock, never())
        .apply(any(), any(), any()); // refers to Point conversion
    verify(eventProducerMock, never()).accept(any());
    assertEquals(
        CityJSONRequest.builder().status(FAILED).step(REQUEST_ACCEPTED).build(),
        savedFailedRequestCaptor.getValue());
  }

  @Test
  void skips_failing_address_and_produces_event_computing_with_remaining_ones() {
    var requestIdentifier = randomUUID().toString();
    var communityOwnerId = randomUUID().toString();
    var geocodableAddress = "geocodable " + randomUUID();
    var ungeocodableAddress = "ungeocodable " + randomUUID();
    var request =
        CityJSONRequest.builder().id(requestIdentifier).communityOwnerId(communityOwnerId).build();
    var convertedAddressFeature = mock(Feature.class);
    when(cityJSONRequestRepositoryMock.findByIdAndCommunityOwnerId(
            requestIdentifier, communityOwnerId))
        .thenReturn(Optional.of(request));
    when(featureAddressConverterMock.apply(geocodableAddress, BUILDING))
        .thenReturn(convertedAddressFeature);
    when(featureAddressConverterMock.apply(ungeocodableAddress, BUILDING))
        .thenThrow(
            new BadRequestException("No building found for address : " + ungeocodableAddress));
    when(cityJSONRequestRepositoryMock.save(any()))
        .thenAnswer(invocationOnMock -> invocationOnMock.getArgument(0));

    assertDoesNotThrow(
        () ->
            subject.accept(
                ThreeDMultipleAddressRequested.builder()
                    .requestIdentifier(requestIdentifier)
                    .communityOwnerId(communityOwnerId)
                    .addresses(List.of(ungeocodableAddress, geocodableAddress))
                    .build()));

    var savedRequestCaptor = ArgumentCaptor.forClass(CityJSONRequest.class);
    verify(cityJSONRequestRepositoryMock, times(1)).save(savedRequestCaptor.capture());
    var savedRequest = savedRequestCaptor.getValue();
    assertEquals(List.of(convertedAddressFeature), savedRequest.getDelimitations());
    assertEquals(POINTS_CLOUD_PRE_PROCESSING, savedRequest.getStep());
    assertNotEquals(FAILED, savedRequest.getStatus());
    verify(eventProducerMock, times(1)).accept(any());
  }

  @SneakyThrows
  @Test
  void skips_failing_point_and_produces_event_computing_with_remaining_ones() {
    var requestIdentifier = randomUUID().toString();
    var communityOwnerId = randomUUID().toString();
    var request =
        CityJSONRequest.builder().id(requestIdentifier).communityOwnerId(communityOwnerId).build();
    var convertibleLongitude = 0.2492928974906442;
    var convertibleLatitude = 46.65193378162583;
    var unconvertibleLongitude = 1.0;
    var unconvertibleLatitude = 2.0;
    var convertiblePoint =
        new Point()
            .coordinates(
                List.of(
                    BigDecimal.valueOf(convertibleLongitude),
                    BigDecimal.valueOf(convertibleLatitude)));
    var unconvertiblePoint =
        new Point()
            .coordinates(
                List.of(
                    BigDecimal.valueOf(unconvertibleLongitude),
                    BigDecimal.valueOf(unconvertibleLatitude)));
    var convertedFeatureFromPoints = mock(Feature.class);
    when(cityJSONRequestRepositoryMock.findByIdAndCommunityOwnerId(
            requestIdentifier, communityOwnerId))
        .thenReturn(Optional.of(request));
    when(featureAddressConverterMock.apply(null, convertibleLongitude, convertibleLatitude))
        .thenReturn(convertedFeatureFromPoints);
    when(featureAddressConverterMock.apply(null, unconvertibleLongitude, unconvertibleLatitude))
        .thenThrow(new BadRequestException("No building found for coordinates"));
    when(cityJSONRequestRepositoryMock.save(any()))
        .thenAnswer(invocationOnMock -> invocationOnMock.getArgument(0));

    assertDoesNotThrow(
        () ->
            subject.accept(
                ThreeDMultipleAddressRequested.builder()
                    .requestIdentifier(requestIdentifier)
                    .communityOwnerId(communityOwnerId)
                    .addresses(null)
                    .points(List.of(unconvertiblePoint, convertiblePoint))
                    .build()));

    var savedRequestCaptor = ArgumentCaptor.forClass(CityJSONRequest.class);
    verify(cityJSONRequestRepositoryMock, times(1)).save(savedRequestCaptor.capture());
    var savedRequest = savedRequestCaptor.getValue();
    var expectedPointFeature =
        new Feature(
            null,
            HOUSES_0.getZoomLevel(),
            new HashMap<>(),
            Feature.FeatureGeometry.builder()
                .geometryType(POINT)
                .actualInstanceStringValue(new ObjectMapper().writeValueAsString(convertiblePoint))
                .build());
    assertEquals(
        List.of(
            new FeatureWithDelimitation(expectedPointFeature, List.of(convertedFeatureFromPoints))),
        savedRequest.getFeaturesWithDelimitation());
    assertNotEquals(FAILED, savedRequest.getStatus());
    verify(eventProducerMock, times(1)).accept(any());
  }

  @Test
  void throws_exception_when_request_not_find_from_db() {
    var requestIdentifier = randomUUID().toString();
    var communityOwnerId = randomUUID().toString();
    when(cityJSONRequestRepositoryMock.findByIdAndCommunityOwnerId(
            requestIdentifier, communityOwnerId))
        .thenReturn(Optional.empty());

    var actual =
        assertThrows(
            NoSuchElementException.class,
            () ->
                subject.accept(
                    ThreeDMultipleAddressRequested.builder()
                        .requestIdentifier(requestIdentifier)
                        .communityOwnerId(communityOwnerId)
                        .addresses(null)
                        .build()));

    assertEquals("No value present", actual.getMessage());
    verify(featureAddressConverterMock, never())
        .apply(any(), any(), any()); // refers to Point conversion
  }

  @Test
  void produces_event_computing_when_address_conversion_succeeds() {
    var requestIdentifier = randomUUID().toString();
    var communityOwnerId = randomUUID().toString();
    var someAddress = "random " + randomUUID();
    var request =
        CityJSONRequest.builder().id(requestIdentifier).communityOwnerId(communityOwnerId).build();
    var convertedAddressFeature = mock(Feature.class);
    var expectedCityJSONRequestSaved =
        CityJSONRequest.builder()
            .id(requestIdentifier)
            .communityOwnerId(communityOwnerId)
            .delimitations(List.of(convertedAddressFeature))
            .step(POINTS_CLOUD_PRE_PROCESSING)
            .build();
    when(cityJSONRequestRepositoryMock.findByIdAndCommunityOwnerId(
            requestIdentifier, communityOwnerId))
        .thenReturn(Optional.of(request));
    when(featureAddressConverterMock.apply(someAddress, BUILDING))
        .thenReturn(convertedAddressFeature);
    when(cityJSONRequestRepositoryMock.save(any()))
        .thenAnswer(invocationOnMock -> invocationOnMock.getArgument(0));

    assertDoesNotThrow(
        () ->
            subject.accept(
                ThreeDMultipleAddressRequested.builder()
                    .requestIdentifier(requestIdentifier)
                    .communityOwnerId(communityOwnerId)
                    .addresses(List.of(someAddress))
                    .build()));

    var savedRequestWithDelimitation = ArgumentCaptor.forClass(CityJSONRequest.class);
    verify(cityJSONRequestRepositoryMock, times(1)).save(savedRequestWithDelimitation.capture());
    assertEquals(expectedCityJSONRequestSaved, savedRequestWithDelimitation.getValue());

    var listCaptor = ArgumentCaptor.forClass(List.class);
    verify(featureAddressConverterMock, never())
        .apply(any(), any(), any()); // refers to Point conversion
    verify(eventProducerMock, times(1)).accept(listCaptor.capture());
    var actualRequestCreatedEvent = (CityJSONRequestCreated) listCaptor.getValue().getFirst();
    assertEquals(
        CityJSONRequestCreated.builder()
            .requestId(requestIdentifier)
            .communityOwnerId(communityOwnerId)
            .build(),
        actualRequestCreatedEvent);
  }

  @SneakyThrows
  @Test
  void produces_event_computing_when_points_conversion_succeeds() {
    var requestIdentifier = randomUUID().toString();
    var communityOwnerId = randomUUID().toString();
    var request =
        CityJSONRequest.builder().id(requestIdentifier).communityOwnerId(communityOwnerId).build();
    var longitude = 0.2492928974906442;
    var latitude = 46.65193378162583;
    var providedPointGeometry =
        new Point()
            .coordinates(List.of(BigDecimal.valueOf(longitude), BigDecimal.valueOf(latitude)));
    var convertedFeatureFromPoints = mock(Feature.class);
    var expectedCityJSONRequestSaved =
        CityJSONRequest.builder()
            .id(requestIdentifier)
            .communityOwnerId(communityOwnerId)
            .featuresWithDelimitation(
                List.of(
                    new FeatureWithDelimitation(
                        new Feature(
                            null,
                            HOUSES_0.getZoomLevel(),
                            new HashMap<>(),
                            Feature.FeatureGeometry.builder()
                                .geometryType(POINT)
                                .actualInstanceStringValue(
                                    new ObjectMapper().writeValueAsString(providedPointGeometry))
                                .build()),
                        List.of(convertedFeatureFromPoints))))
            .build();
    when(cityJSONRequestRepositoryMock.findByIdAndCommunityOwnerId(
            requestIdentifier, communityOwnerId))
        .thenReturn(Optional.of(request));
    when(featureAddressConverterMock.apply(null, longitude, latitude))
        .thenReturn(convertedFeatureFromPoints);
    when(cityJSONRequestRepositoryMock.save(any()))
        .thenAnswer(invocationOnMock -> invocationOnMock.getArgument(0));

    assertDoesNotThrow(
        () ->
            subject.accept(
                ThreeDMultipleAddressRequested.builder()
                    .requestIdentifier(requestIdentifier)
                    .communityOwnerId(communityOwnerId)
                    .addresses(null)
                    .points(List.of(providedPointGeometry))
                    .build()));

    var savedRequestWithDelimitation = ArgumentCaptor.forClass(CityJSONRequest.class);
    verify(cityJSONRequestRepositoryMock, times(1)).save(savedRequestWithDelimitation.capture());
    assertEquals(expectedCityJSONRequestSaved, savedRequestWithDelimitation.getValue());

    var listCaptor = ArgumentCaptor.forClass(List.class);
    verify(eventProducerMock, times(1)).accept(listCaptor.capture());
    var actualRequestCreatedEvent = (CityJSONRequestCreated) listCaptor.getValue().getFirst();
    assertEquals(
        CityJSONRequestCreated.builder()
            .requestId(requestIdentifier)
            .communityOwnerId(communityOwnerId)
            .build(),
        actualRequestCreatedEvent);
  }

  @Test
  void do_not_produces_event_computing_when_points_conversion_fails() {
    var requestIdentifier = randomUUID().toString();
    var communityOwnerId = randomUUID().toString();
    var request = CityJSONRequest.builder().build();
    var longitude = 0.2492928974906442;
    var latitude = 46.65193378162583;
    var providedPointGeometry =
        new Point()
            .coordinates(List.of(BigDecimal.valueOf(longitude), BigDecimal.valueOf(latitude)));
    when(cityJSONRequestRepositoryMock.findByIdAndCommunityOwnerId(
            requestIdentifier, communityOwnerId))
        .thenReturn(Optional.of(request));
    when(featureAddressConverterMock.apply(null, longitude, latitude))
        .thenThrow(new ApiException(SERVER_EXCEPTION, "Unable to convert address to feature"));
    when(cityJSONRequestRepositoryMock.save(any()))
        .thenAnswer(invocationOnMock -> invocationOnMock.getArgument(0));

    assertDoesNotThrow(
        () ->
            subject.accept(
                ThreeDMultipleAddressRequested.builder()
                    .requestIdentifier(requestIdentifier)
                    .communityOwnerId(communityOwnerId)
                    .addresses(null)
                    .points(List.of(providedPointGeometry))
                    .build()));
    var savedFailedRequestCaptor = ArgumentCaptor.forClass(CityJSONRequest.class);
    verify(cityJSONRequestRepositoryMock, times(1)).save(savedFailedRequestCaptor.capture());
    verify(eventProducerMock, never()).accept(any());
    assertEquals(
        CityJSONRequest.builder().status(FAILED).build(), savedFailedRequestCaptor.getValue());
  }
}
