package app.bpartners.geojobs.unit;

import static app.bpartners.geojobs.model.lidar.LidarProcessorType.THREE_D_BAG_ROOFER;
import static app.bpartners.geojobs.repository.model.cityjson.CityJSONRequestStatus.FAILED;
import static app.bpartners.geojobs.repository.model.cityjson.CityJSONRequestStatus.FINISHED;
import static app.bpartners.geojobs.repository.model.cityjson.CityJSONRequestStep.GEOMETRY_CONSTRUCTION;
import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

import app.bpartners.geojobs.endpoint.event.EventProducer;
import app.bpartners.geojobs.endpoint.event.model.CityJSONRequestCreated;
import app.bpartners.geojobs.endpoint.rest.controller.mapper.FeatureMapper;
import app.bpartners.geojobs.file.bucket.BucketComponent;
import app.bpartners.geojobs.repository.CityJSONRequestRepository;
import app.bpartners.geojobs.repository.CommunityAuthorizationRepository;
import app.bpartners.geojobs.repository.model.cityjson.CityJSON;
import app.bpartners.geojobs.repository.model.cityjson.CityJSONRequest;
import app.bpartners.geojobs.repository.model.community.CommunityAuthorization;
import app.bpartners.geojobs.service.CityJSON3DBagRooferProcessor;
import app.bpartners.geojobs.service.cityjson.LidarDataToCityJsonProcessor;
import app.bpartners.geojobs.service.cityjson.texture.CityJsonTextureComputer;
import app.bpartners.geojobs.service.event.CityJSONRequestCreatedService;
import app.bpartners.geojobs.service.lidar.LasRoofsPointsExtractor;
import jakarta.persistence.EntityManager;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

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
  CityJsonTextureComputer textureComputerMock = mock(CityJsonTextureComputer.class);

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
          textureComputerMock);

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
    when(cityJSONRequestBuilderMock.cityJsons(List.of(cityJSONMock)))
        .thenReturn(cityJSONRequestBuilderMock);
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
        .thenThrow(new RuntimeException());
    when(cityJSONRequestMock.toBuilder()).thenReturn(cityJSONRequestBuilderMock);
    when(cityJSONRequestBuilderMock.status(FAILED)).thenReturn(cityJSONRequestBuilderMock);
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
  }
}
