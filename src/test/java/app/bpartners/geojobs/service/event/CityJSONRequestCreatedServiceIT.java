package app.bpartners.geojobs.service.event;

import static app.bpartners.geojobs.endpoint.event.EventStack.EVENT_STACK_2;
import static app.bpartners.geojobs.endpoint.rest.controller.mapper.FeatureMapper.toDomainFeature;
import static app.bpartners.geojobs.endpoint.rest.model.ModelName.TOITURE;
import static app.bpartners.geojobs.endpoint.rest.security.model.Authority.Role.ROLE_INSURANCE;
import static app.bpartners.geojobs.model.geometry.GeometryFactory.geometryFactory;
import static app.bpartners.geojobs.model.lidar.planes.model.LasRoofDelimitationType.ENTIRE_ROOF_DELIMITATION;
import static app.bpartners.geojobs.repository.model.SurfaceUnit.SQUARE_METER;
import static app.bpartners.geojobs.repository.model.cityjson.CityJSONRequestStatus.*;
import static java.time.Instant.now;
import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.Mockito.*;

import app.bpartners.geojobs.conf.FacadeIT;
import app.bpartners.geojobs.endpoint.event.EventProducer;
import app.bpartners.geojobs.endpoint.event.model.CityJSONRequestCreated;
import app.bpartners.geojobs.endpoint.event.model.ThreeDRequestMonitoringTriggered;
import app.bpartners.geojobs.endpoint.rest.controller.mapper.FeatureMapper;
import app.bpartners.geojobs.file.bucket.BucketComponent;
import app.bpartners.geojobs.model.lidar.LasPointGeometry;
import app.bpartners.geojobs.model.lidar.planes.model.DelimitedRoofPoints;
import app.bpartners.geojobs.repository.CityJSONRequestRepository;
import app.bpartners.geojobs.repository.CommunityAuthorizationRepository;
import app.bpartners.geojobs.repository.model.Feature;
import app.bpartners.geojobs.repository.model.cityjson.CityJSONRequest;
import app.bpartners.geojobs.repository.model.community.CommunityAuthorization;
import app.bpartners.geojobs.service.cityjson.LidarDataToCityJsonProcessor;
import app.bpartners.geojobs.service.lidar.LasRoofsPointsExtractor;
import app.bpartners.geojobs.service.lidar.PointsExtractionResult;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Polygon;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

@Disabled("TODO: internal Lidar not processed anymore")
class CityJSONRequestCreatedServiceIT extends FacadeIT {
  @MockBean BucketComponent bucketComponentMock;
  @Autowired CityJSONRequestCreatedService subject;
  @MockBean LasRoofsPointsExtractor pointsExtractor;
  @MockBean LidarDataToCityJsonProcessor cityJsonProcessorMock;
  @MockBean private EventProducer eventProducerMock;
  @Autowired FeatureMapper featureMapper;
  @Autowired CityJSONRequestRepository cityJSONRequestRepository;
  @Autowired CommunityAuthorizationRepository communityAuthorizationRepository;

  private static final String COMMUNITY_OWNER_ID = randomUUID().toString();
  private static final String REQUEST_ID = randomUUID().toString();

  @BeforeEach()
  void setUp() {
    when(bucketComponentMock.upload(any(), any())).thenReturn(mock());
    communityAuthorizationRepository.save(
        CommunityAuthorization.builder()
            .id(COMMUNITY_OWNER_ID)
            .name(randomUUID().toString())
            .email(randomUUID().toString())
            .role(ROLE_INSURANCE)
            .maxSurfaceUnit(SQUARE_METER)
            .detectableModels(List.of(TOITURE))
            .dashboardApiKey(randomUUID().toString())
            .build());
    cityJSONRequestRepository.save(request());
  }

  @AfterEach()
  void cleanUp() {
    cityJSONRequestRepository.deleteById(REQUEST_ID);
    communityAuthorizationRepository.deleteById(COMMUNITY_OWNER_ID);
  }

  @Test
  void generate_ok() {
    var validResult = validResult();
    when(pointsExtractor.apply(eq(ENTIRE_ROOF_DELIMITATION), anySet())).thenReturn(validResult);
    when(cityJsonProcessorMock.apply(any(), any(PointsExtractionResult.class))).thenReturn(mock());

    subject.accept(
        CityJSONRequestCreated.builder()
            .requestId(REQUEST_ID)
            .communityOwnerId(COMMUNITY_OWNER_ID)
            .build());

    var actualRequest =
        cityJSONRequestRepository
            .findByIdAndCommunityOwnerId(REQUEST_ID, COMMUNITY_OWNER_ID)
            .orElseThrow();
    assertEquals(FINISHED, actualRequest.getStatus());

    var listCaptor = ArgumentCaptor.forClass(List.class);
    verify(eventProducerMock).accept(listCaptor.capture());
    var actualMonitoringTriggered =
        (ThreeDRequestMonitoringTriggered) listCaptor.getValue().getFirst();
    assertEquals(
        new ThreeDRequestMonitoringTriggered(REQUEST_ID, COMMUNITY_OWNER_ID),
        actualMonitoringTriggered);
    assertEquals(EVENT_STACK_2, actualMonitoringTriggered.getEventStack());
    assertEquals(
        Duration.ofMinutes(2), actualMonitoringTriggered.maxConsumerBackoffBetweenRetries());
    assertEquals(Duration.ofMinutes(5), actualMonitoringTriggered.maxConsumerDuration());
  }

  @Test
  void should_be_unavailable_when_lidar_data_is_unavailable() {
    when(pointsExtractor.apply(eq(ENTIRE_ROOF_DELIMITATION), anySet()))
        .thenReturn(unavailableResult());

    subject.accept(
        CityJSONRequestCreated.builder()
            .requestId(REQUEST_ID)
            .communityOwnerId(COMMUNITY_OWNER_ID)
            .build());

    var actualRequest =
        cityJSONRequestRepository
            .findByIdAndCommunityOwnerId(REQUEST_ID, COMMUNITY_OWNER_ID)
            .orElseThrow();

    assertEquals(UNAVAILABLE, actualRequest.getStatus());
    verify(cityJsonProcessorMock, never()).apply(any(), any(PointsExtractionResult.class));
  }

  @Test
  void should_be_failed_when_lidar_data_is_extraction_error() {
    when(pointsExtractor.apply(eq(ENTIRE_ROOF_DELIMITATION), anySet()))
        .thenThrow(RuntimeException.class);

    var request =
        CityJSONRequestCreated.builder()
            .requestId(REQUEST_ID)
            .communityOwnerId(COMMUNITY_OWNER_ID)
            .build();
    assertThrows(RuntimeException.class, () -> subject.accept(request));

    var actualRequest =
        cityJSONRequestRepository
            .findByIdAndCommunityOwnerId(REQUEST_ID, COMMUNITY_OWNER_ID)
            .orElseThrow();

    assertEquals(FAILED, actualRequest.getStatus());
    verify(cityJsonProcessorMock, never()).apply(any(), any(PointsExtractionResult.class));
  }

  private static PointsExtractionResult validResult() {
    var delimitedRoofPoints = mock(DelimitedRoofPoints.class);
    when(delimitedRoofPoints.getPoints()).thenReturn(List.of(mock(LasPointGeometry.class)));
    return new PointsExtractionResult(Map.of(mock(Envelope.class), delimitedRoofPoints));
  }

  private static PointsExtractionResult unavailableResult() {
    return new PointsExtractionResult(Map.of());
  }

  private CityJSONRequest request() {
    return CityJSONRequest.builder()
        .id(REQUEST_ID)
        .communityOwnerId(COMMUNITY_OWNER_ID)
        .delimitations(List.of(roofFeature()))
        .creationDatetime(now())
        .build();
  }

  private Feature roofFeature() {
    var restFeature = featureMapper.toRest(roofPolygon(), 20, randomUUID().toString());
    return toDomainFeature(restFeature);
  }

  private Polygon roofPolygon() {
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
    return geometryFactory.createPolygon(roof1Coordinates);
  }
}
