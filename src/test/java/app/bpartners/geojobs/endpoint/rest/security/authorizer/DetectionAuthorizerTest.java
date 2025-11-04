package app.bpartners.geojobs.endpoint.rest.security.authorizer;

import static app.bpartners.geojobs.endpoint.rest.security.model.Authority.Role.ROLE_ADMIN;
import static app.bpartners.geojobs.endpoint.rest.security.model.Authority.Role.ROLE_COMMUNITY;
import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import app.bpartners.geojobs.endpoint.rest.controller.mapper.DetectableObjectTypeMapper;
import app.bpartners.geojobs.endpoint.rest.model.CreateDetection;
import app.bpartners.geojobs.endpoint.rest.model.Feature;
import app.bpartners.geojobs.endpoint.rest.security.model.Authority;
import app.bpartners.geojobs.endpoint.rest.security.model.Principal;
import app.bpartners.geojobs.endpoint.rest.validator.FeatureTypeChecker;
import app.bpartners.geojobs.repository.CommunityAuthorizationRepository;
import app.bpartners.geojobs.repository.DetectionRepository;
import app.bpartners.geojobs.repository.model.community.CommunityAuthorization;
import app.bpartners.geojobs.repository.model.detection.Detection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DetectionAuthorizerTest {
  private static final String COMMUNITY_ID = "dummyId";
  CreateDetection createDetection = mock();
  CommunityZoneSurfaceAuthorizer communityZoneSurfaceAuthorizer = mock();
  CommunityZoneAuthorizer communityZoneAuthorizer = mock();
  CommunityDetectableObjectTypeAuthorizer communityDetectableObjectTypeAuthorizer = mock();
  CommunityAuthorizationRepository communityAuthRepository = mock();
  DetectionRepository detectionRepository = mock();
  DetectionOwnerAuthorizer detectionOwnerAuthorizer = mock();
  DetectableObjectTypeMapper detectableObjectTypeMapperMock = mock();
  FeatureTypeChecker featureTypeChecker = mock();
  DetectionAuthorizer subject =
      new DetectionAuthorizer(
          communityDetectableObjectTypeAuthorizer,
          communityAuthRepository,
          communityZoneAuthorizer,
          communityZoneSurfaceAuthorizer,
          detectionOwnerAuthorizer,
          detectionRepository,
          detectableObjectTypeMapperMock,
          featureTypeChecker);

  CommunityAuthorization communityAuthorization =
      CommunityAuthorization.builder().id(COMMUNITY_ID).authorizedZones(List.of()).build();

  @BeforeEach
  void setup() {
    when(communityAuthRepository.findByApiKey(any()))
        .thenReturn(Optional.of(communityAuthorization));
    when(createDetection.getGeoJsonZone()).thenReturn(List.of(mock(Feature.class)));
    when(createDetection.getDetectableObjectModel()).thenReturn(mock());
  }

  @Test
  void should_accept_directly_admin_api_key() {
    assertDoesNotThrow(
        () -> subject.accept(randomUUID().toString(), createDetection, useRole(ROLE_ADMIN)));
  }

  @Test
  void should_accept_community_if_authorization_is_correct() {
    when(detectionRepository.findByEndToEndIdAndCommunityOwnerId(any(), any()))
        .thenReturn(Optional.empty());
    doNothing().when(communityZoneSurfaceAuthorizer).accept(any(), any());
    doNothing().when(communityZoneAuthorizer).accept(any(), any(), any());
    doNothing().when(communityDetectableObjectTypeAuthorizer).accept(any(), any());
    when(detectableObjectTypeMapperMock.mapDefaultConfigurationsFromModel(any(), any()))
        .thenReturn(List.of());

    assertDoesNotThrow(
        () -> subject.accept(randomUUID().toString(), createDetection, useRole(ROLE_COMMUNITY)));
    verify(detectionOwnerAuthorizer, never()).accept(any(), any());
  }

  @Test
  void should_check_only_owner_if_endToEndId_already_exist_and_accept_it() {
    when(detectionRepository.findByEndToEndIdAndCommunityOwnerId(any(), any()))
        .thenReturn(Optional.of(Detection.builder().communityOwnerId(COMMUNITY_ID).build()));
    assertDoesNotThrow(
        () -> subject.authorizeCommunity(randomUUID().toString(), useRole(ROLE_COMMUNITY)));

    verify(detectionOwnerAuthorizer, times(1)).accept(any(), any());
  }

  private Principal useRole(Authority.Role role) {
    return new Principal("dummyKey", Set.of(new Authority(role)));
  }
}
