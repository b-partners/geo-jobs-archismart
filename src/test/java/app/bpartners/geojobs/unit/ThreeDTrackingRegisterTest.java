package app.bpartners.geojobs.unit;

import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import app.bpartners.geojobs.model.exception.DashboardUnavailableException;
import app.bpartners.geojobs.model.exception.NotFoundException;
import app.bpartners.geojobs.repository.CityJSONRequestRepository;
import app.bpartners.geojobs.repository.CommunityAuthorizationRepository;
import app.bpartners.geojobs.repository.model.Feature;
import app.bpartners.geojobs.repository.model.cityjson.CityJSONDelimitationObjectType;
import app.bpartners.geojobs.repository.model.cityjson.CityJSONRequest;
import app.bpartners.geojobs.repository.model.community.CommunityAuthorization;
import app.bpartners.geojobs.repository.model.dashboard.DashboardRegistrationType;
import app.bpartners.geojobs.service.DashboardRegistrationFailureService;
import app.bpartners.geojobs.service.ThreeDTrackingRegister;
import app.bpartners.geojobs.service.dashboard.AnalysisProjectApi;
import app.bpartners.geojobs.service.dashboard.AnalysisProjectBilling;
import app.bpartners.geojobs.service.dashboard.AnalysisProjectKeys;
import app.bpartners.geojobs.service.dashboard.component.AnalysisProject;
import app.bpartners.geojobs.service.dashboard.component.Register3d;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;

class ThreeDTrackingRegisterTest {
  static final String POLYGON =
      "{\"type\":\"Polygon\",\"coordinates\":"
          + "[[[2.0,48.0],[2.001,48.0],[2.001,48.001],[2.0,48.001],[2.0,48.0]]]}";

  AnalysisProjectApi analysisProjectApiMock = mock();
  DashboardRegistrationFailureService dashboardRegistrationFailureServiceMock = mock();
  CommunityAuthorizationRepository communityAuthorizationRepositoryMock = mock();
  CityJSONRequestRepository cityJSONRequestRepositoryMock = mock();
  ThreeDTrackingRegister subject =
      new ThreeDTrackingRegister(
          analysisProjectApiMock,
          new AnalysisProjectBilling(analysisProjectApiMock, communityAuthorizationRepositoryMock),
          dashboardRegistrationFailureServiceMock,
          communityAuthorizationRepositoryMock,
          cityJSONRequestRepositoryMock);

  final String requestId = randomUUID().toString();
  final String communityOwnerId = randomUUID().toString();
  final String dashboardApiKey = "dashboardApiKey";

  @Test
  void register_3d_with_geometry_key_and_mark_request_registered() {
    givenRequest(requestBuilder().build());
    givenCommunity(CommunityAuthorization.builder().dashboardApiKey(dashboardApiKey).build());
    var analysisProject = mock(AnalysisProject.class);
    when(analysisProjectApiMock.register3d(any(), any(), any())).thenReturn(analysisProject);

    subject.accept(requestId, communityOwnerId, null);

    var projectKeyCaptor = ArgumentCaptor.forClass(String.class);
    verify(analysisProjectApiMock)
        .register3d(eq(dashboardApiKey), projectKeyCaptor.capture(), eq(new Register3d(null)));
    assertEquals(
        AnalysisProjectKeys.of(List.of(polygonFeature())).orElseThrow(),
        projectKeyCaptor.getValue());
    verify(cityJSONRequestRepositoryMock).markDashboardRegistered(eq(requestId), any());
  }

  @Test
  void register_3d_on_the_provided_project_with_its_external_reference() {
    givenRequest(requestBuilder().projectKey("session-1").externalReference("crm-42").build());
    givenCommunity(CommunityAuthorization.builder().dashboardApiKey(dashboardApiKey).build());

    subject.accept(requestId, communityOwnerId, null);

    verify(analysisProjectApiMock)
        .register3d(eq(dashboardApiKey), eq("session-1"), eq(new Register3d(null, "crm-42")));
  }

  @Test
  void register_3d_falls_back_to_a_request_key_without_geometry() {
    givenRequest(requestBuilder().delimitations(List.of()).build());
    givenCommunity(CommunityAuthorization.builder().dashboardApiKey(dashboardApiKey).build());

    subject.accept(requestId, communityOwnerId, null);

    verify(analysisProjectApiMock)
        .register3d(eq(dashboardApiKey), eq("city-json:" + requestId), any());
  }

  @Test
  void register_3d_of_several_geometries_with_a_request_key() {
    var other =
        Feature.builder()
            .geometry(
                Feature.FeatureGeometry.builder()
                    .actualInstanceStringValue("{\"type\":\"Point\",\"coordinates\":[2.5,48.5]}")
                    .build())
            .build();
    givenRequest(requestBuilder().delimitations(List.of(polygonFeature(), other)).build());
    givenCommunity(CommunityAuthorization.builder().dashboardApiKey(dashboardApiKey).build());

    subject.accept(requestId, communityOwnerId, null);

    verify(analysisProjectApiMock)
        .register3d(eq(dashboardApiKey), eq("city-json:" + requestId), any());
  }

  @Test
  void register_3d_pan_by_pan_as_a_single_geometry() {
    var otherPan =
        Feature.builder()
            .geometry(
                Feature.FeatureGeometry.builder()
                    .actualInstanceStringValue(
                        "{\"type\":\"Polygon\",\"coordinates\":[[[2.01,48.0],[2.011,48.0],"
                            + "[2.011,48.001],[2.01,48.001],[2.01,48.0]]]}")
                    .build())
            .build();
    var pans = List.of(polygonFeature(), otherPan);
    givenRequest(
        requestBuilder()
            .delimitationObjectType(CityJSONDelimitationObjectType.BUILDING_ROOF_SEGMENT_FACE)
            .delimitations(pans)
            .build());
    givenCommunity(CommunityAuthorization.builder().dashboardApiKey(dashboardApiKey).build());

    subject.accept(requestId, communityOwnerId, null);

    verify(analysisProjectApiMock)
        .register3d(eq(dashboardApiKey), eq(AnalysisProjectKeys.of(pans).orElseThrow()), any());
  }

  @Test
  void skip_request_already_registered() {
    givenRequest(requestBuilder().dashboardRegistrationDatetime(Instant.now()).build());

    subject.accept(requestId, communityOwnerId, null);

    verify(analysisProjectApiMock, never()).register3d(any(), any(), any());
    verify(cityJSONRequestRepositoryMock, never()).markDashboardRegistered(any(), any());
  }

  @Test
  void skip_community_without_dashboard_api_key() {
    givenRequest(requestBuilder().build());
    givenCommunity(CommunityAuthorization.builder().build());

    subject.accept(requestId, communityOwnerId, null);

    verify(analysisProjectApiMock, never()).register3d(any(), any(), any());
    verify(cityJSONRequestRepositoryMock, never()).markDashboardRegistered(any(), any());
  }

  @Test
  void register_integration_test_community_like_any_other() {
    givenRequest(requestBuilder().build());
    givenCommunity(
        CommunityAuthorization.builder()
            .dashboardApiKey(dashboardApiKey)
            .integrationTestUsage(true)
            .build());
    when(analysisProjectApiMock.register3d(any(), any(), any()))
        .thenReturn(mock(AnalysisProject.class));

    subject.accept(requestId, communityOwnerId, null);

    verify(analysisProjectApiMock).register3d(eq(dashboardApiKey), any(), any());
    verify(cityJSONRequestRepositoryMock).markDashboardRegistered(eq(requestId), any());
  }

  @Test
  void insufficient_credits_are_swallowed_and_request_stays_unregistered() {
    givenRequest(requestBuilder().build());
    givenCommunity(CommunityAuthorization.builder().dashboardApiKey(dashboardApiKey).build());
    when(analysisProjectApiMock.register3d(any(), any(), any()))
        .thenThrow(clientError(HttpStatus.PAYMENT_REQUIRED));

    assertDoesNotThrow(() -> subject.accept(requestId, communityOwnerId, null));

    verify(cityJSONRequestRepositoryMock, never()).markDashboardRegistered(any(), any());
  }

  @Test
  void unreachable_dashboard_persists_the_registration_and_keeps_request_unregistered() {
    givenRequest(requestBuilder().build());
    givenCommunity(CommunityAuthorization.builder().dashboardApiKey(dashboardApiKey).build());
    var unreachable = new ResourceAccessException("connection refused");
    when(analysisProjectApiMock.register3d(any(), any(), any())).thenThrow(unreachable);

    assertDoesNotThrow(() -> subject.accept(requestId, communityOwnerId, null));

    verify(dashboardRegistrationFailureServiceMock)
        .recordFailure(
            eq(DashboardRegistrationType.THREE_D_3D),
            eq(requestId),
            eq(communityOwnerId),
            eq(AnalysisProjectKeys.of(List.of(polygonFeature())).orElseThrow()),
            eq(new Register3d(null)),
            any(DashboardUnavailableException.class));
    verify(cityJSONRequestRepositoryMock, never()).markDashboardRegistered(any(), any());
  }

  @Test
  void server_error_is_handled_like_an_unreachable_dashboard() {
    givenRequest(requestBuilder().build());
    givenCommunity(CommunityAuthorization.builder().dashboardApiKey(dashboardApiKey).build());
    when(analysisProjectApiMock.register3d(any(), any(), any()))
        .thenThrow(
            HttpServerErrorException.create(
                HttpStatus.BAD_GATEWAY, "error", HttpHeaders.EMPTY, new byte[0], null));

    assertDoesNotThrow(() -> subject.accept(requestId, communityOwnerId, null));

    verify(dashboardRegistrationFailureServiceMock)
        .recordFailure(any(), any(), any(), any(), any(), any(DashboardUnavailableException.class));
  }

  @Test
  void normal_registration_does_not_touch_the_failures() {
    givenRequest(requestBuilder().build());
    givenCommunity(CommunityAuthorization.builder().dashboardApiKey(dashboardApiKey).build());

    subject.accept(requestId, communityOwnerId, null);

    verifyNoInteractions(dashboardRegistrationFailureServiceMock);
  }

  @Test
  void successful_replay_resolves_the_failure() {
    givenRequest(requestBuilder().build());
    givenCommunity(CommunityAuthorization.builder().dashboardApiKey(dashboardApiKey).build());

    subject.accept(requestId, communityOwnerId, "failure");

    verify(dashboardRegistrationFailureServiceMock).resolve("failure");
    verify(cityJSONRequestRepositoryMock).markDashboardRegistered(eq(requestId), any());
  }

  @Test
  void replay_with_insufficient_credits_resolves_the_failure() {
    givenRequest(requestBuilder().build());
    givenCommunity(CommunityAuthorization.builder().dashboardApiKey(dashboardApiKey).build());
    when(analysisProjectApiMock.register3d(any(), any(), any()))
        .thenThrow(clientError(HttpStatus.PAYMENT_REQUIRED));

    subject.accept(requestId, communityOwnerId, "failure");

    verify(dashboardRegistrationFailureServiceMock).resolve("failure");
  }

  @Test
  void replay_of_a_community_no_longer_billed_resolves_the_failure() {
    givenRequest(requestBuilder().build());
    givenCommunity(CommunityAuthorization.builder().build());

    subject.accept(requestId, communityOwnerId, "failure");

    verify(dashboardRegistrationFailureServiceMock).resolve("failure");
  }

  @Test
  void replay_of_an_already_registered_request_resolves_the_failure() {
    givenRequest(requestBuilder().dashboardRegistrationDatetime(Instant.now()).build());

    subject.accept(requestId, communityOwnerId, "failure");

    verify(dashboardRegistrationFailureServiceMock).resolve("failure");
    verify(analysisProjectApiMock, never()).register3d(any(), any(), any());
  }

  @Test
  void replay_with_a_still_unreachable_dashboard_records_again_without_resolving() {
    givenRequest(requestBuilder().build());
    givenCommunity(CommunityAuthorization.builder().dashboardApiKey(dashboardApiKey).build());
    when(analysisProjectApiMock.register3d(any(), any(), any()))
        .thenThrow(new ResourceAccessException("connection refused"));

    subject.accept(requestId, communityOwnerId, "failure");

    verify(dashboardRegistrationFailureServiceMock)
        .recordFailure(any(), any(), any(), any(), any(), any(DashboardUnavailableException.class));
    verify(dashboardRegistrationFailureServiceMock, never()).resolve(any());
  }

  @Test
  void other_api_failures_are_rethrown_to_be_retried() {
    givenRequest(requestBuilder().build());
    givenCommunity(CommunityAuthorization.builder().dashboardApiKey(dashboardApiKey).build());
    when(analysisProjectApiMock.register3d(any(), any(), any()))
        .thenThrow(clientError(HttpStatus.FORBIDDEN));

    assertThrows(
        HttpClientErrorException.class, () -> subject.accept(requestId, communityOwnerId, null));
    verify(cityJSONRequestRepositoryMock, never()).markDashboardRegistered(any(), any());
  }

  @Test
  void unknown_request_is_not_found() {
    when(cityJSONRequestRepositoryMock.findByIdAndCommunityOwnerId(requestId, communityOwnerId))
        .thenReturn(Optional.empty());

    assertThrows(NotFoundException.class, () -> subject.accept(requestId, communityOwnerId, null));
  }

  @Test
  void billing_applies_to_every_community_with_a_dashboard_key() {
    assertTrue(
        AnalysisProjectBilling.isBilled(
            CommunityAuthorization.builder().dashboardApiKey(dashboardApiKey).build()));
    assertTrue(
        AnalysisProjectBilling.isBilled(
            CommunityAuthorization.builder()
                .dashboardApiKey(dashboardApiKey)
                .integrationTestUsage(true)
                .build()));
    assertEquals(
        false,
        AnalysisProjectBilling.isBilled(
            CommunityAuthorization.builder().dashboardApiKey(" ").build()));
  }

  private CityJSONRequest.CityJSONRequestBuilder requestBuilder() {
    return CityJSONRequest.builder()
        .id(requestId)
        .communityOwnerId(communityOwnerId)
        .delimitations(List.of(polygonFeature()));
  }

  private void givenRequest(CityJSONRequest request) {
    when(cityJSONRequestRepositoryMock.findByIdAndCommunityOwnerId(requestId, communityOwnerId))
        .thenReturn(Optional.of(request));
  }

  private void givenCommunity(CommunityAuthorization community) {
    when(communityAuthorizationRepositoryMock.findById(communityOwnerId))
        .thenReturn(Optional.of(community));
  }

  private static Feature polygonFeature() {
    return Feature.builder()
        .geometry(Feature.FeatureGeometry.builder().actualInstanceStringValue(POLYGON).build())
        .build();
  }

  private static HttpClientErrorException clientError(HttpStatus status) {
    return HttpClientErrorException.create(status, "error", HttpHeaders.EMPTY, new byte[0], null);
  }
}
