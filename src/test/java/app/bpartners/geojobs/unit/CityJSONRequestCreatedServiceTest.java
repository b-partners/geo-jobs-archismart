package app.bpartners.geojobs.unit;

import static app.bpartners.geojobs.model.lidar.LidarProcessorType.DEFAULT;
import static app.bpartners.geojobs.model.lidar.LidarProcessorType.SAFE_MODE;
import static app.bpartners.geojobs.model.lidar.LidarProcessorType.THREE_D_BAG_ROOFER;
import static app.bpartners.geojobs.repository.model.cityjson.CityJSONRequestStatus.FAILED;
import static app.bpartners.geojobs.repository.model.cityjson.CityJSONRequestStatus.FINISHED;
import static app.bpartners.geojobs.repository.model.cityjson.CityJSONRequestStatus.UNAVAILABLE;
import static app.bpartners.geojobs.repository.model.cityjson.CityJSONRequestStep.GEOMETRY_CONSTRUCTION;
import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

import app.bpartners.geojobs.endpoint.event.EventProducer;
import app.bpartners.geojobs.endpoint.event.model.CityJSONRequestCreated;
import app.bpartners.geojobs.endpoint.rest.controller.v1.mapper.FeatureMapper;
import app.bpartners.geojobs.file.bucket.BucketComponent;
import app.bpartners.geojobs.repository.CityJSONRequestRepository;
import app.bpartners.geojobs.repository.CommunityAuthorizationRepository;
import app.bpartners.geojobs.repository.model.cityjson.CityJSON;
import app.bpartners.geojobs.repository.model.cityjson.CityJSONDelimitationObjectType;
import app.bpartners.geojobs.repository.model.cityjson.CityJSONRequest;
import app.bpartners.geojobs.repository.model.cityjson.CityJSONRequestStatus;
import app.bpartners.geojobs.repository.model.cityjson.CityJSONRequestStep;
import app.bpartners.geojobs.repository.model.community.CommunityAuthorization;
import app.bpartners.geojobs.service.CityJSON3DBagRooferProcessor;
import app.bpartners.geojobs.service.CityJSONSafeModeProcessor;
import app.bpartners.geojobs.service.CityJSONThreedProcessor;
import app.bpartners.geojobs.service.ThreedGenerationResult;
import app.bpartners.geojobs.service.cityjson.LidarDataToCityJsonProcessor;
import app.bpartners.geojobs.service.cityjson.texture.CityJsonTextureComputer;
import app.bpartners.geojobs.service.event.CityJSONRequestCreatedService;
import app.bpartners.geojobs.service.lidar.LasRoofsPointsExtractor;
import app.bpartners.geojobs.service.threed.exception.ThreedApiException;
import jakarta.persistence.EntityManager;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpStatus;

class CityJSONRequestCreatedServiceTest {
  CityJSONRequestRepository cityJSONRequestRepositoryMock = mock();
  LasRoofsPointsExtractor lasRoofsPointsExtractorMock = mock();
  LidarDataToCityJsonProcessor lidarDataToCityJsonProcessorMock = mock();
  FeatureMapper featureMapperMock = mock();
  EntityManager entityManagerMock = mock();
  BucketComponent bucketComponentMock = mock();
  EventProducer eventProducerMock = mock();
  CommunityAuthorizationRepository communityAuthorizationRepositoryMock = mock();
  CityJSON3DBagRooferProcessor cityJson3DBagRooferProcessorMock = mock();
  CityJSONSafeModeProcessor cityJSONSafeModeProcessor = mock();
  CityJsonTextureComputer textureComputerMock = mock(CityJsonTextureComputer.class);
  CityJSONThreedProcessor cityJSONThreedProcessorMock = mock();

  CityJSONRequestCreatedService subject =
      new CityJSONRequestCreatedService(
          cityJSONRequestRepositoryMock,
          lasRoofsPointsExtractorMock,
          lidarDataToCityJsonProcessorMock,
          featureMapperMock,
          entityManagerMock,
          bucketComponentMock,
          eventProducerMock,
          communityAuthorizationRepositoryMock,
          cityJson3DBagRooferProcessorMock,
          cityJSONSafeModeProcessor,
          textureComputerMock,
          cityJSONThreedProcessorMock);

  @BeforeEach
  void setUp() {
    doNothing().when(entityManagerMock).clear();
  }

  @Test
  void process_city_json_request_through_3d_bag_roofer() {
    var communityOwnerId = randomUUID().toString();
    var requestId = randomUUID().toString();

    var communityAuthorizationMock = mock(CommunityAuthorization.class);
    var cityJSONRequestMock = mock(CityJSONRequest.class);
    var cityJSONMock = mock(CityJSON.class);
    var cityJSONRequestBuilderMock = mock(CityJSONRequest.CityJSONRequestBuilder.class);
    when(cityJSONRequestBuilderMock.status(FINISHED)).thenReturn(cityJSONRequestBuilderMock);
    when(cityJSONRequestBuilderMock.step(GEOMETRY_CONSTRUCTION))
        .thenReturn(cityJSONRequestBuilderMock);
    when(cityJSONRequestBuilderMock.message(any())).thenReturn(cityJSONRequestBuilderMock);
    when(cityJSONRequestBuilderMock.cityJsons(List.of(cityJSONMock)))
        .thenReturn(cityJSONRequestBuilderMock);
    when(cityJSONRequestBuilderMock.warnings(any())).thenReturn(cityJSONRequestBuilderMock);
    when(cityJSONRequestBuilderMock.build()).thenReturn(cityJSONRequestMock);
    when(cityJSONRequestMock.toBuilder()).thenReturn(cityJSONRequestBuilderMock);
    when(communityAuthorizationMock.isIntegrationTestUsage()).thenReturn(true);
    when(communityAuthorizationRepositoryMock.findById(communityOwnerId))
        .thenReturn(Optional.of(communityAuthorizationMock));
    when(cityJSONRequestRepositoryMock.findByIdAndCommunityOwnerId(requestId, communityOwnerId))
        .thenReturn(Optional.of(cityJSONRequestMock));
    when(cityJson3DBagRooferProcessorMock.apply(cityJSONRequestMock))
        .thenReturn(List.of(cityJSONMock));
    when(cityJSONRequestRepositoryMock.save(any()))
        .thenAnswer(invocationOnMock -> invocationOnMock.getArgument(0));
    when(textureComputerMock.applyTexture(any(), any()))
        .thenAnswer(invocation -> invocation.getArgument(1));

    assertDoesNotThrow(
        () ->
            subject.accept(
                CityJSONRequestCreated.builder()
                    .requestId(requestId)
                    .communityOwnerId(communityOwnerId)
                    .lidarProcessorType(THREE_D_BAG_ROOFER)
                    .build()));

    var cityJsonRequestCaptor = ArgumentCaptor.forClass(CityJSONRequest.class);
    verify(cityJSONRequestRepositoryMock).save(cityJsonRequestCaptor.capture());
    assertEquals(cityJSONRequestMock, cityJsonRequestCaptor.getValue());
  }

  @Test
  void fail_process_when_3d_bag_raises_exception() {
    var communityOwnerId = randomUUID().toString();
    var requestId = randomUUID().toString();

    var communityAuthorizationMock = mock(CommunityAuthorization.class);
    var cityJSONRequestMock = mock(CityJSONRequest.class);
    var cityJSONRequestBuilderMock = mock(CityJSONRequest.CityJSONRequestBuilder.class);
    when(communityAuthorizationRepositoryMock.findById(communityOwnerId))
        .thenReturn(Optional.of(communityAuthorizationMock));
    when(cityJSONRequestRepositoryMock.findByIdAndCommunityOwnerId(requestId, communityOwnerId))
        .thenReturn(Optional.of(cityJSONRequestMock));
    when(cityJson3DBagRooferProcessorMock.apply(cityJSONRequestMock))
        .thenThrow(
            new IllegalStateException("Invalid CityJSON file", new RuntimeException("root")));
    when(cityJSONRequestMock.toBuilder()).thenReturn(cityJSONRequestBuilderMock);
    when(cityJSONRequestBuilderMock.status(FAILED)).thenReturn(cityJSONRequestBuilderMock);
    when(cityJSONRequestBuilderMock.message(any())).thenReturn(cityJSONRequestBuilderMock);
    when(cityJSONRequestBuilderMock.step(GEOMETRY_CONSTRUCTION))
        .thenReturn(cityJSONRequestBuilderMock);
    when(cityJSONRequestBuilderMock.build()).thenReturn(cityJSONRequestMock);

    assertDoesNotThrow(
        () ->
            subject.accept(
                CityJSONRequestCreated.builder()
                    .requestId(requestId)
                    .communityOwnerId(communityOwnerId)
                    .lidarProcessorType(THREE_D_BAG_ROOFER)
                    .build()));

    var cityJsonRequestCaptor = ArgumentCaptor.forClass(CityJSONRequest.class);
    verify(cityJSONRequestRepositoryMock).save(cityJsonRequestCaptor.capture());
    assertEquals(cityJSONRequestMock, cityJsonRequestCaptor.getValue());
    verify(cityJSONRequestBuilderMock).message("Invalid CityJSON file: root");
  }

  private CityJSONRequest.CityJSONRequestBuilder runWithRequest(
      CityJSONRequestCreatedService service,
      CityJSONRequest request,
      CityJSONRequestStatus status,
      CityJSONRequestStep step,
      boolean rethrows) {
    var communityOwnerId = randomUUID().toString();
    var requestId = randomUUID().toString();
    var builderMock = mock(CityJSONRequest.CityJSONRequestBuilder.class);
    when(communityAuthorizationRepositoryMock.findById(communityOwnerId))
        .thenReturn(Optional.of(mock(CommunityAuthorization.class)));
    when(cityJSONRequestRepositoryMock.findByIdAndCommunityOwnerId(requestId, communityOwnerId))
        .thenReturn(Optional.of(request));
    when(request.toBuilder()).thenReturn(builderMock);
    when(builderMock.status(status)).thenReturn(builderMock);
    when(builderMock.step(step)).thenReturn(builderMock);
    when(builderMock.message(any())).thenReturn(builderMock);
    when(builderMock.cityJsons(any())).thenReturn(builderMock);
    when(builderMock.warnings(any())).thenReturn(builderMock);
    when(builderMock.build()).thenReturn(request);

    var event =
        CityJSONRequestCreated.builder()
            .requestId(requestId)
            .communityOwnerId(communityOwnerId)
            .build();
    if (rethrows) {
      assertThrows(RuntimeException.class, () -> service.accept(event));
    } else {
      assertDoesNotThrow(() -> service.accept(event));
    }

    verify(cityJSONRequestRepositoryMock).save(request);
    return builderMock;
  }

  @Test
  void fail_process_by_safe_mode_stores_message() {
    var request = mock(CityJSONRequest.class);
    when(request.getLidarProcessorType()).thenReturn(SAFE_MODE);
    when(cityJSONSafeModeProcessor.apply(request)).thenThrow(new RuntimeException("boom"));

    var builder = runWithRequest(subject, request, FAILED, GEOMETRY_CONSTRUCTION, false);

    verify(builder).message("boom");
  }

  @Test
  void fail_process_by_threed_stores_message() {
    var request = mock(CityJSONRequest.class);
    when(request.getDelimitationObjectType())
        .thenReturn(CityJSONDelimitationObjectType.BUILDING_ROOF_SEGMENT_FACE);
    when(cityJSONThreedProcessorMock.applyWithDiagnostics(request))
        .thenThrow(new IllegalStateException("no delimitation"));

    var builder = runWithRequest(subject, request, FAILED, GEOMETRY_CONSTRUCTION, true);

    verify(builder).message("no delimitation");
  }

  @Test
  void fail_process_by_threed_with_no_lidar_data_maps_to_unavailable() {
    var request = mock(CityJSONRequest.class);
    when(request.getDelimitationObjectType())
        .thenReturn(CityJSONDelimitationObjectType.BUILDING_ROOF_SEGMENT_FACE);
    when(cityJSONThreedProcessorMock.applyWithDiagnostics(request))
        .thenThrow(
            new ThreedApiException(
                HttpStatus.UNPROCESSABLE_ENTITY,
                "NO_LIDAR_DATA_FOUND",
                "No lidar data found",
                "Threed API error (HTTP 422) : No lidar data found"));

    var builder = runWithRequest(subject, request, UNAVAILABLE, GEOMETRY_CONSTRUCTION, true);

    verify(builder).message("Threed API error (HTTP 422) : No lidar data found");
  }

  @Test
  void default_lidar_processor_type_routes_to_threed() {
    var request = mock(CityJSONRequest.class);
    when(request.getLidarProcessorType()).thenReturn(DEFAULT);
    when(cityJSONThreedProcessorMock.applyWithDiagnostics(request))
        .thenReturn(new ThreedGenerationResult(List.of(mock(CityJSON.class)), null, null));

    var builder = runWithRequest(subject, request, FINISHED, GEOMETRY_CONSTRUCTION, false);

    verify(builder).message(null);
    verify(builder).warnings(null);
  }

  @Test
  void degraded_generation_from_threed_still_succeeds_with_message_and_warnings() {
    var request = mock(CityJSONRequest.class);
    when(request.getLidarProcessorType()).thenReturn(DEFAULT);
    when(cityJSONThreedProcessorMock.applyWithDiagnostics(request))
        .thenReturn(
            new ThreedGenerationResult(
                List.of(mock(CityJSON.class)),
                "1 roof panel(s) had no lidar coverage",
                List.of("GROUND_ONLY_PANELS")));

    var builder = runWithRequest(subject, request, FINISHED, GEOMETRY_CONSTRUCTION, false);

    verify(builder).message("1 roof panel(s) had no lidar coverage");
    verify(builder).warnings(List.of("GROUND_ONLY_PANELS"));
  }

  @Test
  void fail_process_by_threed_for_default_lidar_processor_type_stores_message() {
    var request = mock(CityJSONRequest.class);
    when(request.getLidarProcessorType()).thenReturn(DEFAULT);
    when(cityJSONThreedProcessorMock.applyWithDiagnostics(request))
        .thenThrow(new RuntimeException("boom"));

    var builder = runWithRequest(subject, request, FAILED, GEOMETRY_CONSTRUCTION, true);

    verify(builder).message("boom");
  }
}
