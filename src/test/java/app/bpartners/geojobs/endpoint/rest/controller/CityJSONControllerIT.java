package app.bpartners.geojobs.endpoint.rest.controller;

import static app.bpartners.geojobs.endpoint.rest.model.CityJSONRequestStatus.PROCESSING;
import static app.bpartners.geojobs.endpoint.rest.model.DelimitationObjectType.BUILDING_ROOF;
import static app.bpartners.geojobs.endpoint.rest.model.DelimitationType.PARCEL_FREE_DELIMITATION;
import static app.bpartners.geojobs.model.geometry.GeometryFactory.geometryFactory;
import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import app.bpartners.geojobs.conf.FacadeIT;
import app.bpartners.geojobs.endpoint.event.EventProducer;
import app.bpartners.geojobs.endpoint.event.model.CityJSONRequestCreated;
import app.bpartners.geojobs.endpoint.rest.controller.mapper.FeatureMapper;
import app.bpartners.geojobs.endpoint.rest.model.*;
import app.bpartners.geojobs.endpoint.rest.security.AuthProvider;
import app.bpartners.geojobs.endpoint.rest.security.model.Principal;
import app.bpartners.geojobs.file.bucket.BucketComponent;
import app.bpartners.geojobs.model.exception.BadRequestException;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import software.amazon.awssdk.services.eventbridge.EventBridgeClient;
import software.amazon.awssdk.services.eventbridge.model.PutEventsRequest;
import software.amazon.awssdk.services.eventbridge.model.PutEventsResponse;

class CityJSONControllerIT extends FacadeIT {
  @Autowired CityJSONController subject;
  @Autowired FeatureMapper featureMapper;

  @MockBean AuthProvider authProviderMock;
  @MockBean BucketComponent bucketComponentMock;
  @MockBean EventProducer<CityJSONRequestCreated> eventProducerMock;
  @MockBean EventBridgeClient eventBridgeClient;
  private static final String ADMIN_KEY = randomUUID().toString();

  @BeforeEach
  void setUp() {
    when(bucketComponentMock.upload(any(), any())).thenReturn(mock());
    doNothing().when(eventProducerMock).accept(anyList());

    var principal = mock(Principal.class);
    when(principal.isAdmin()).thenReturn(true);
    when(principal.getPassword()).thenReturn(ADMIN_KEY);
    when(authProviderMock.getPrincipal()).thenReturn(principal);
    when(eventBridgeClient.putEvents(any(PutEventsRequest.class)))
        .thenReturn(PutEventsResponse.builder().failedEntryCount(0).build());
  }

  @Test
  void should_persist_and_get_custom_knn_and_complexity_factor() {
    var requestIdentifier = randomUUID().toString();
    var payload =
        new ThreeDRequest()
            .delimitations(List.of(feature()))
            .delimitationObjectType(BUILDING_ROOF)
            .delimitationType(PARCEL_FREE_DELIMITATION)
            .knn(8)
            .complexityFactor(BigDecimal.valueOf(0.88));

    var actual = subject.request3DFileOnDelimitations(payload, requestIdentifier);

    var actualFromDb = subject.getRequested3DFileById(requestIdentifier);

    assertEquals(8, actual.getKnn());
    assertEquals(BigDecimal.valueOf(0.88), actual.getComplexityFactor());
    assertEquals(8, actualFromDb.getKnn());
    assertEquals(BigDecimal.valueOf(0.88), actualFromDb.getComplexityFactor());
  }

  @Test
  void should_be_processed_if_new_request() {
    var payload =
        new CreateCityJSONRequest().id(randomUUID().toString()).delimitations(List.of(feature()));

    var expected =
        new CityJSONRequest()
            .id(payload.getId())
            .delimitations(payload.getDelimitations())
            .status(PROCESSING);

    var actual = subject.processCityJSONRequest(payload, payload.getId());

    assertEquals(expected.getStatus(), actual.getStatus());
    assertEquals(expected.getId(), actual.getId());
    assertEquals(expected.getCityJsons(), actual.getCityJsons());
    assertEquals(PROCESSING, actual.getStatus());
  }

  @Test
  void should_throw_if_empty_delimitations_if_given() {
    var payloadId = randomUUID().toString();
    var payloadWithEmptyDelimitations =
        new CreateCityJSONRequest().id(payloadId).delimitations(List.of());

    var exception =
        assertThrows(
            BadRequestException.class,
            () -> subject.processCityJSONRequest(payloadWithEmptyDelimitations, payloadId));
    assertTrue(exception.getMessage().contains("CityJSONRequest.delimitations"));
  }

  @Test
  void should_throw_if_null_delimitations_if_given() {
    var payloadId = randomUUID().toString();
    var payloadWithNullDelimitations = new CreateCityJSONRequest().id(payloadId);

    var exception =
        assertThrows(
            BadRequestException.class,
            () -> subject.processCityJSONRequest(payloadWithNullDelimitations, payloadId));
    assertTrue(exception.getMessage().contains("CityJSONRequest.delimitations"));
  }

  @Test
  void should_throw_if_delimitations_if_is_more_than_5() {
    var payloadId = randomUUID().toString();
    var payloadWithNullDelimitations =
        new CreateCityJSONRequest()
            .delimitations(List.of(mock(), mock(), mock(), mock(), mock(), mock()))
            .id(payloadId);

    var exception =
        assertThrows(
            BadRequestException.class,
            () -> subject.processCityJSONRequest(payloadWithNullDelimitations, payloadId));
    assertTrue(exception.getMessage().contains("more than 5"));
  }

  @Test
  void should_get_correctly_processed_city_json_request() {
    var payloadId = randomUUID().toString();
    var payload = new CreateCityJSONRequest().id(payloadId).delimitations(List.of(feature()));

    subject.processCityJSONRequest(payload, payloadId);

    var expected =
        new CityJSONRequest()
            .id(payload.getId())
            .delimitations(payload.getDelimitations())
            .status(PROCESSING);

    var actual = subject.getById(payloadId);

    assertEquals(expected.getStatus(), actual.getStatus());
    assertEquals(expected.getId(), actual.getId());
    assertEquals(expected.getCityJsons(), actual.getCityJsons());
    assertEquals(PROCESSING, actual.getStatus());
  }

  private Feature feature() {
    var roof1Coordinates =
        new Coordinate[] {
          new Coordinate(2.243891733457616, 48.82448842864014),
          new Coordinate(2.243947393505863, 48.82437718542337),
          new Coordinate(2.244038835011281, 48.82440597780899),
          new Coordinate(2.2440209442821413, 48.82445309258651),
          new Coordinate(2.244197863717403, 48.8244975898354),
          new Coordinate(2.24422768160008, 48.82447010624497),
          new Coordinate(2.24432906240051, 48.824487119898066),
          new Coordinate(2.244263463059525, 48.82456695311532),
          new Coordinate(2.243891733457616, 48.82448842864014)
        };
    var polygon = geometryFactory.createPolygon(roof1Coordinates);
    return featureMapper.toRest(polygon, 20, randomUUID().toString());
  }
}
