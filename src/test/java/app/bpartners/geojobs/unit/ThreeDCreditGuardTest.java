package app.bpartners.geojobs.unit;

import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import app.bpartners.geojobs.model.exception.PaymentRequiredException;
import app.bpartners.geojobs.repository.CommunityAuthorizationRepository;
import app.bpartners.geojobs.repository.model.Feature;
import app.bpartners.geojobs.repository.model.cityjson.CityJSONDelimitationObjectType;
import app.bpartners.geojobs.repository.model.cityjson.CityJSONRequest;
import app.bpartners.geojobs.repository.model.community.CommunityAuthorization;
import app.bpartners.geojobs.service.ThreeDCreditGuard;
import app.bpartners.geojobs.service.dashboard.AnalysisProjectApi;
import app.bpartners.geojobs.service.dashboard.AnalysisProjectBilling;
import app.bpartners.geojobs.service.dashboard.AnalysisProjectKeys;
import app.bpartners.geojobs.service.dashboard.component.AnalysisProject;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class ThreeDCreditGuardTest {
  static final String SQUARE =
      "{\"type\":\"Polygon\",\"coordinates\":"
          + "[[[2.0,48.0],[2.001,48.0],[2.001,48.001],[2.0,48.001],[2.0,48.0]]]}";
  static final String OTHER_SQUARE =
      "{\"type\":\"Polygon\",\"coordinates\":"
          + "[[[2.01,48.0],[2.011,48.0],[2.011,48.001],[2.01,48.001],[2.01,48.0]]]}";

  AnalysisProjectApi analysisProjectApiMock = mock();
  CommunityAuthorizationRepository communityAuthorizationRepositoryMock = mock();
  ThreeDCreditGuard subject =
      new ThreeDCreditGuard(
          new AnalysisProjectBilling(analysisProjectApiMock, communityAuthorizationRepositoryMock));

  final String communityOwnerId = randomUUID().toString();
  final String dashboardApiKey = "dashboardApiKey";
  final CityJSONRequest request =
      CityJSONRequest.builder()
          .id(randomUUID().toString())
          .communityOwnerId(communityOwnerId)
          .delimitations(List.of(polygonFeature(SQUARE)))
          .build();
  final CityJSONRequest bulkRequest =
      request.toBuilder()
          .delimitations(List.of(polygonFeature(SQUARE), polygonFeature(OTHER_SQUARE)))
          .build();

  @Test
  void accept_when_credits_are_sufficient() {
    givenCommunity(CommunityAuthorization.builder().dashboardApiKey(dashboardApiKey).build());
    when(analysisProjectApiMock.get(dashboardApiKey, individualKey())).thenReturn(project(true));

    assertDoesNotThrow(() -> subject.accept(request));
  }

  @Test
  void provided_project_key_is_checked_instead_of_the_geometry_key() {
    givenCommunity(CommunityAuthorization.builder().dashboardApiKey(dashboardApiKey).build());
    when(analysisProjectApiMock.get(dashboardApiKey, "session-1")).thenReturn(project(false));

    assertThrows(
        PaymentRequiredException.class,
        () -> subject.accept(request.toBuilder().projectKey("session-1").build()));
  }

  @Test
  void refuse_when_credits_are_insufficient() {
    givenCommunity(CommunityAuthorization.builder().dashboardApiKey(dashboardApiKey).build());
    when(analysisProjectApiMock.get(any(), any())).thenReturn(project(false));

    assertThrows(PaymentRequiredException.class, () -> subject.accept(request));
  }

  @Test
  void bulk_request_is_keyed_by_request_and_checked_like_any_other() {
    givenCommunity(CommunityAuthorization.builder().dashboardApiKey(dashboardApiKey).build());
    when(analysisProjectApiMock.get(dashboardApiKey, "city-json:" + bulkRequest.getId()))
        .thenReturn(project(false));

    assertThrows(PaymentRequiredException.class, () -> subject.accept(bulkRequest));
  }

  @Test
  void bulk_request_is_accepted_when_dashboard_cannot_be_reached() {
    givenCommunity(CommunityAuthorization.builder().dashboardApiKey(dashboardApiKey).build());
    when(analysisProjectApiMock.get(any(), any())).thenThrow(new RuntimeException("down"));

    assertDoesNotThrow(() -> subject.accept(bulkRequest));
  }

  @Test
  void pan_by_pan_request_is_individual_whatever_its_number_of_pans() {
    var panByPanRequest =
        bulkRequest.toBuilder()
            .delimitationObjectType(CityJSONDelimitationObjectType.BUILDING_ROOF_SEGMENT_FACE)
            .build();
    givenCommunity(CommunityAuthorization.builder().dashboardApiKey(dashboardApiKey).build());
    when(analysisProjectApiMock.get(any(), any())).thenThrow(new RuntimeException("down"));

    assertDoesNotThrow(() -> subject.accept(panByPanRequest));
  }

  @Test
  void individual_request_is_accepted_when_dashboard_cannot_be_reached() {
    givenCommunity(CommunityAuthorization.builder().dashboardApiKey(dashboardApiKey).build());
    when(analysisProjectApiMock.get(any(), any())).thenThrow(new RuntimeException("down"));

    assertDoesNotThrow(() -> subject.accept(request));
  }

  @Test
  void skip_community_not_billed_on_dashboard() {
    givenCommunity(CommunityAuthorization.builder().build());

    assertDoesNotThrow(() -> subject.accept(request));

    verify(analysisProjectApiMock, never()).get(any(), any());
  }

  @Test
  void skip_request_without_community_owner() {
    var withoutOwner = request.toBuilder().communityOwnerId(null).build();

    assertDoesNotThrow(() -> subject.accept(withoutOwner));

    verify(analysisProjectApiMock, never()).get(any(), any());
  }

  private String individualKey() {
    return AnalysisProjectKeys.of(request.getRequestDelimitations()).orElseThrow();
  }

  private static Feature polygonFeature(String geometryJson) {
    return Feature.builder()
        .geometry(Feature.FeatureGeometry.builder().actualInstanceStringValue(geometryJson).build())
        .build();
  }

  @Test
  void accept_when_dashboard_gives_no_credit_balance() {
    givenCommunity(CommunityAuthorization.builder().dashboardApiKey(dashboardApiKey).build());
    when(analysisProjectApiMock.get(any(), any()))
        .thenReturn(new AnalysisProject("key", false, false, false, List.of(), null, null));

    assertDoesNotThrow(() -> subject.accept(request));
  }

  private void givenCommunity(CommunityAuthorization community) {
    when(communityAuthorizationRepositoryMock.findById(communityOwnerId))
        .thenReturn(Optional.of(community));
  }

  private static AnalysisProject project(boolean creditsSufficient) {
    return new AnalysisProject(
        "key", false, false, false, List.of("ANALYSIS_2D", "MODEL_3D"), 1L, creditsSufficient);
  }
}
