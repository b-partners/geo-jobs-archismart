package app.bpartners.geojobs.service.event;

import static java.lang.System.currentTimeMillis;
import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import app.bpartners.geojobs.endpoint.event.model.DetectionTrackingRegistrationRequested;
import app.bpartners.geojobs.model.exception.DashboardUnavailableException;
import app.bpartners.geojobs.repository.CommunityAuthorizationRepository;
import app.bpartners.geojobs.repository.DetectionRepository;
import app.bpartners.geojobs.repository.model.Feature;
import app.bpartners.geojobs.repository.model.community.CommunityAuthorization;
import app.bpartners.geojobs.repository.model.dashboard.DashboardRegistrationType;
import app.bpartners.geojobs.repository.model.detection.Detection;
import app.bpartners.geojobs.service.DashboardRegistrationFailureService;
import app.bpartners.geojobs.service.DetectionAddressResolver;
import app.bpartners.geojobs.service.DetectionTrackingRegister;
import app.bpartners.geojobs.service.dashboard.AnalysisProjectApi;
import app.bpartners.geojobs.service.dashboard.AnalysisProjectBilling;
import app.bpartners.geojobs.service.dashboard.AnalysisProjectKeys;
import app.bpartners.geojobs.service.dashboard.component.AnalysisProject;
import app.bpartners.geojobs.service.dashboard.component.DetectionInitiator;
import app.bpartners.geojobs.service.dashboard.component.Register2d;
import java.util.List;
import java.util.Optional;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;

class DetectionTrackingRegistrationRequestedServiceTest {
  AnalysisProjectApi analysisProjectApiMock = mock();
  DashboardRegistrationFailureService dashboardRegistrationFailureServiceMock = mock();
  CommunityAuthorizationRepository communityAuthorizationRepositoryMock = mock();
  DetectionRepository detectionRepositoryMock = mock();
  DetectionAddressResolver detectionAddressResolverMock = mock();
  DetectionTrackingRegister detectionTrackingRegister =
      new DetectionTrackingRegister(
          analysisProjectApiMock,
          new AnalysisProjectBilling(analysisProjectApiMock, communityAuthorizationRepositoryMock),
          dashboardRegistrationFailureServiceMock,
          communityAuthorizationRepositoryMock,
          detectionRepositoryMock,
          detectionAddressResolverMock);
  DetectionTrackingRegistrationRequestedService subject =
      new DetectionTrackingRegistrationRequestedService(
          detectionTrackingRegister, detectionRepositoryMock);

  final String apiKey = "apiKey";

  @BeforeEach
  void setUp() {
    var analysisProject = mock(AnalysisProject.class);
    when(analysisProjectApiMock.register2d(eq(apiKey), any(), any())).thenReturn(analysisProject);
  }

  @Test
  void register_detection_ok() {
    var detectionIdentifier = randomUUID().toString();
    var communityOwnerId = randomUUID().toString();
    var zoneName = "detection zone name";
    var emailReceiver = "detection email receiver";
    var endToEndId = "detection end to end id";
    var detectionBuilderMock = mock(Detection.DetectionBuilder.class);

    var detectionMock = mock(Detection.class);
    var detectionMockWithRegistrationDatetime = mock(Detection.class);
    when(detectionMock.getId()).thenReturn(detectionIdentifier);
    when(detectionMock.getZoneName()).thenReturn(zoneName);
    when(detectionMock.getEmailReceiver()).thenReturn(emailReceiver);
    when(detectionMock.getEndToEndId()).thenReturn(endToEndId);
    when(detectionMock.getCommunityOwnerId()).thenReturn(communityOwnerId);
    when(detectionAddressResolverMock.apply(detectionMock)).thenReturn(zoneName);
    when(detectionBuilderMock.dashboardRegistrationDatetime(any()))
        .thenReturn(detectionBuilderMock);
    when(detectionMock.toBuilder()).thenReturn(detectionBuilderMock);
    when(detectionBuilderMock.build()).thenReturn(detectionMockWithRegistrationDatetime);

    var dummyCommunityName = "dummy " + currentTimeMillis();
    var communityAuthorization =
        CommunityAuthorization.builder().dashboardApiKey(apiKey).name(dummyCommunityName).build();
    when(detectionRepositoryMock.findById(detectionIdentifier))
        .thenReturn(Optional.of(detectionMock));
    when(detectionRepositoryMock.save(any()))
        .thenAnswer(invocationOnMock -> invocationOnMock.getArgument(0));
    when(communityAuthorizationRepositoryMock.findById(communityOwnerId))
        .thenReturn(Optional.of(communityAuthorization));

    assertDoesNotThrow(() -> registerDetection(detectionIdentifier));

    var projectKeyCaptor = ArgumentCaptor.forClass(String.class);
    var registerCaptor = ArgumentCaptor.forClass(Register2d.class);
    verify(analysisProjectApiMock, only())
        .register2d(eq(apiKey), projectKeyCaptor.capture(), registerCaptor.capture());
    var expectedRegister2d =
        getExpectedRegister2d(
            zoneName,
            emailReceiver,
            endToEndId,
            dummyCommunityName,
            zoneName,
            registerCaptor.getValue());
    verify(detectionRepositoryMock).save(detectionMockWithRegistrationDatetime);

    assertEquals(expectedRegister2d, registerCaptor.getValue());
    assertEquals("detection:" + detectionIdentifier, projectKeyCaptor.getValue());
  }

  private @NotNull Register2d getExpectedRegister2d(
      String zoneName,
      String emailReceiver,
      String endToEndId,
      String communityName,
      String resolvedAddress,
      Register2d actual) {
    return new Register2d(
        endToEndId,
        zoneName,
        resolvedAddress,
        actual.creationDatetime(),
        new DetectionInitiator(communityName, emailReceiver, null));
  }

  @Test
  void register_individual_detection_with_geometry_key() {
    var detectionIdentifier = randomUUID().toString();
    var detectionMock = registrableDetection(detectionIdentifier);
    when(detectionMock.getDomainProvidedGeoJsonZone()).thenReturn(List.of(polygonFeature()));

    subject.accept(new DetectionTrackingRegistrationRequested(detectionIdentifier));

    verify(analysisProjectApiMock)
        .register2d(
            eq(apiKey), eq(AnalysisProjectKeys.of(List.of(polygonFeature())).orElseThrow()), any());
  }

  @Test
  void register_detection_on_the_provided_project_with_its_external_reference() {
    var detectionIdentifier = randomUUID().toString();
    var detectionMock = registrableDetection(detectionIdentifier);
    when(detectionMock.getDomainProvidedGeoJsonZone()).thenReturn(List.of(polygonFeature()));
    when(detectionMock.getProjectKey()).thenReturn("session-1");
    when(detectionMock.getExternalReference()).thenReturn("crm-42");

    subject.accept(new DetectionTrackingRegistrationRequested(detectionIdentifier));

    var registerCaptor = ArgumentCaptor.forClass(Register2d.class);
    verify(analysisProjectApiMock)
        .register2d(eq(apiKey), eq("session-1"), registerCaptor.capture());
    assertEquals("crm-42", registerCaptor.getValue().externalReference());
  }

  @Test
  void register_multi_address_detection_with_a_detection_key_whatever_the_given_zone() {
    var detectionIdentifier = randomUUID().toString();
    var detectionMock = registrableDetection(detectionIdentifier);
    when(detectionMock.getConvertedAddresses()).thenReturn(List.of("address 1", "address 2"));
    when(detectionMock.getDomainProvidedGeoJsonZone()).thenReturn(List.of(polygonFeature()));

    subject.accept(new DetectionTrackingRegistrationRequested(detectionIdentifier));

    verify(analysisProjectApiMock)
        .register2d(eq(apiKey), eq("detection:" + detectionIdentifier), any());
  }

  @Test
  void register_single_address_detection_with_geometry_key() {
    var detectionIdentifier = randomUUID().toString();
    var detectionMock = registrableDetection(detectionIdentifier);
    when(detectionMock.getConvertedAddresses()).thenReturn(List.of("address 1"));
    when(detectionMock.getDomainProvidedGeoJsonZone()).thenReturn(List.of(polygonFeature()));

    subject.accept(new DetectionTrackingRegistrationRequested(detectionIdentifier));

    verify(analysisProjectApiMock)
        .register2d(
            eq(apiKey), eq(AnalysisProjectKeys.of(List.of(polygonFeature())).orElseThrow()), any());
  }

  @Test
  void register_detection_without_converted_addresses_with_geometry_key() {
    var detectionIdentifier = randomUUID().toString();
    var detectionMock = registrableDetection(detectionIdentifier);
    when(detectionMock.getConvertedAddresses()).thenReturn(null);
    when(detectionMock.getDomainProvidedGeoJsonZone()).thenReturn(List.of(polygonFeature()));

    subject.accept(new DetectionTrackingRegistrationRequested(detectionIdentifier));

    verify(analysisProjectApiMock)
        .register2d(
            eq(apiKey), eq(AnalysisProjectKeys.of(List.of(polygonFeature())).orElseThrow()), any());
  }

  @Test
  void register_detection_with_an_empty_zone_with_a_detection_key() {
    var detectionIdentifier = randomUUID().toString();
    var detectionMock = registrableDetection(detectionIdentifier);
    when(detectionMock.getDomainProvidedGeoJsonZone()).thenReturn(List.of());

    subject.accept(new DetectionTrackingRegistrationRequested(detectionIdentifier));

    verify(analysisProjectApiMock)
        .register2d(eq(apiKey), eq("detection:" + detectionIdentifier), any());
  }

  @Test
  void register_detection_with_the_resolved_address() {
    var detectionIdentifier = randomUUID().toString();
    var detectionMock = registrableDetection(detectionIdentifier);
    when(detectionAddressResolverMock.apply(detectionMock)).thenReturn("resolved address");

    subject.accept(new DetectionTrackingRegistrationRequested(detectionIdentifier));

    var registerCaptor = ArgumentCaptor.forClass(Register2d.class);
    verify(analysisProjectApiMock).register2d(eq(apiKey), any(), registerCaptor.capture());
    assertEquals("resolved address", registerCaptor.getValue().address());
  }

  private void registerDetection(String detectionIdentifier) {
    subject.accept(new DetectionTrackingRegistrationRequested(detectionIdentifier));
  }

  private Detection registrableDetection(String detectionIdentifier) {
    var communityOwnerId = randomUUID().toString();
    var detectionBuilderMock = mock(Detection.DetectionBuilder.class);
    var detectionMock = mock(Detection.class);
    when(detectionMock.getId()).thenReturn(detectionIdentifier);
    when(detectionMock.getCommunityOwnerId()).thenReturn(communityOwnerId);
    when(detectionBuilderMock.dashboardRegistrationDatetime(any()))
        .thenReturn(detectionBuilderMock);
    when(detectionMock.toBuilder()).thenReturn(detectionBuilderMock);
    var registeredDetectionMock = mock(Detection.class);
    when(detectionBuilderMock.build()).thenReturn(registeredDetectionMock);
    when(detectionRepositoryMock.findById(detectionIdentifier))
        .thenReturn(Optional.of(detectionMock));
    when(detectionRepositoryMock.save(any()))
        .thenAnswer(invocationOnMock -> invocationOnMock.getArgument(0));
    when(communityAuthorizationRepositoryMock.findById(communityOwnerId))
        .thenReturn(Optional.of(CommunityAuthorization.builder().dashboardApiKey(apiKey).build()));
    return detectionMock;
  }

  private static Feature polygonFeature() {
    return Feature.builder()
        .geometry(
            Feature.FeatureGeometry.builder()
                .actualInstanceStringValue(
                    "{\"type\":\"Polygon\",\"coordinates\":"
                        + "[[[2.0,48.0],[2.001,48.0],[2.001,48.001],[2.0,48.001],[2.0,48.0]]]}")
                .build())
        .build();
  }

  @Test
  void insufficient_credits_are_swallowed_and_detection_stays_unregistered() {
    var detectionIdentifier = randomUUID().toString();
    registrableDetection(detectionIdentifier);
    when(analysisProjectApiMock.register2d(any(), any(), any()))
        .thenThrow(
            HttpClientErrorException.create(
                HttpStatus.PAYMENT_REQUIRED, "error", HttpHeaders.EMPTY, new byte[0], null));

    assertDoesNotThrow(() -> registerDetection(detectionIdentifier));

    verify(detectionRepositoryMock, never()).save(any());
  }

  @Test
  void unreachable_dashboard_persists_the_registration_and_keeps_detection_unregistered() {
    var detectionIdentifier = randomUUID().toString();
    var detectionMock = registrableDetection(detectionIdentifier);
    when(analysisProjectApiMock.register2d(any(), any(), any()))
        .thenThrow(new ResourceAccessException("connection refused"));

    assertDoesNotThrow(() -> registerDetection(detectionIdentifier));

    var communityOwnerId = detectionMock.getCommunityOwnerId();
    verify(dashboardRegistrationFailureServiceMock)
        .recordFailure(
            eq(DashboardRegistrationType.DETECTION_2D),
            eq(detectionIdentifier),
            eq(communityOwnerId),
            eq("detection:" + detectionIdentifier),
            any(Register2d.class),
            any(DashboardUnavailableException.class));
    verify(detectionRepositoryMock, never()).save(any());
  }

  @Test
  void normal_registration_does_not_touch_the_failures() {
    var detectionIdentifier = randomUUID().toString();
    registrableDetection(detectionIdentifier);

    subject.accept(new DetectionTrackingRegistrationRequested(detectionIdentifier));

    verifyNoInteractions(dashboardRegistrationFailureServiceMock);
  }

  @Test
  void successful_replay_resolves_the_failure() {
    var detectionIdentifier = randomUUID().toString();
    registrableDetection(detectionIdentifier);

    subject.accept(new DetectionTrackingRegistrationRequested(detectionIdentifier, "failure"));

    verify(dashboardRegistrationFailureServiceMock).resolve("failure");
  }

  @Test
  void replay_with_insufficient_credits_resolves_the_failure() {
    var detectionIdentifier = randomUUID().toString();
    registrableDetection(detectionIdentifier);
    when(analysisProjectApiMock.register2d(any(), any(), any()))
        .thenThrow(
            HttpClientErrorException.create(
                HttpStatus.PAYMENT_REQUIRED, "error", HttpHeaders.EMPTY, new byte[0], null));

    subject.accept(new DetectionTrackingRegistrationRequested(detectionIdentifier, "failure"));

    verify(dashboardRegistrationFailureServiceMock).resolve("failure");
  }

  @Test
  void replay_of_an_already_registered_detection_resolves_the_failure() {
    var detectionIdentifier = randomUUID().toString();
    var detectionMock = registrableDetection(detectionIdentifier);
    when(detectionMock.isDashboardRegistrationCompleted()).thenReturn(true);

    subject.accept(new DetectionTrackingRegistrationRequested(detectionIdentifier, "failure"));

    verify(dashboardRegistrationFailureServiceMock).resolve("failure");
    verify(analysisProjectApiMock, never()).register2d(any(), any(), any());
  }

  @Test
  void replay_with_a_still_unreachable_dashboard_records_again_without_resolving() {
    var detectionIdentifier = randomUUID().toString();
    registrableDetection(detectionIdentifier);
    when(analysisProjectApiMock.register2d(any(), any(), any()))
        .thenThrow(new ResourceAccessException("connection refused"));

    subject.accept(new DetectionTrackingRegistrationRequested(detectionIdentifier, "failure"));

    verify(dashboardRegistrationFailureServiceMock)
        .recordFailure(any(), any(), any(), any(), any(), any(DashboardUnavailableException.class));
    verify(dashboardRegistrationFailureServiceMock, never()).resolve(any());
  }

  @Test
  void other_client_errors_are_rethrown_to_be_retried() {
    var detectionIdentifier = randomUUID().toString();
    registrableDetection(detectionIdentifier);
    when(analysisProjectApiMock.register2d(any(), any(), any()))
        .thenThrow(
            HttpClientErrorException.create(
                HttpStatus.FORBIDDEN, "error", HttpHeaders.EMPTY, new byte[0], null));

    assertThrows(HttpClientErrorException.class, () -> registerDetection(detectionIdentifier));
    verify(detectionRepositoryMock, never()).save(any());
  }

  @Test
  void register_integration_test_community_like_any_other() {
    var detectionIdentifier = randomUUID().toString();
    var detectionMock = registrableDetection(detectionIdentifier);
    when(communityAuthorizationRepositoryMock.findById(detectionMock.getCommunityOwnerId()))
        .thenReturn(
            Optional.of(
                CommunityAuthorization.builder()
                    .dashboardApiKey(apiKey)
                    .integrationTestUsage(true)
                    .build()));

    subject.accept(new DetectionTrackingRegistrationRequested(detectionIdentifier));

    verify(analysisProjectApiMock).register2d(eq(apiKey), any(), any());
  }

  @Test
  void skip_community_not_billed_on_dashboard() {
    var detectionIdentifier = randomUUID().toString();
    var detectionMock = registrableDetection(detectionIdentifier);
    when(communityAuthorizationRepositoryMock.findById(detectionMock.getCommunityOwnerId()))
        .thenReturn(Optional.of(CommunityAuthorization.builder().build()));

    subject.accept(new DetectionTrackingRegistrationRequested(detectionIdentifier));

    verify(analysisProjectApiMock, never()).register2d(any(), any(), any());
    verify(detectionRepositoryMock, never()).save(any());
  }

  @Test
  void register_detection_fail_on_api_failure() {
    var detectionIdentifier = randomUUID().toString();
    var detectionMock = mock(Detection.class);
    when(detectionMock.getId()).thenReturn(detectionIdentifier);
    when(detectionMock.getCommunityOwnerId()).thenReturn(randomUUID().toString());
    reset(analysisProjectApiMock);
    when(detectionRepositoryMock.findById(detectionIdentifier))
        .thenReturn(Optional.of(detectionMock));
    when(communityAuthorizationRepositoryMock.findById(any()))
        .thenReturn(Optional.of(CommunityAuthorization.builder().dashboardApiKey(apiKey).build()));
    when(analysisProjectApiMock.register2d(any(), any(), any())).thenThrow(new RuntimeException());

    assertThrows(RuntimeException.class, () -> registerDetection(detectionIdentifier));
  }

  @Test
  void register_detection_fail_on_api_key_not_found() {
    var detectionIdentifier = randomUUID().toString();
    var detectionMock = mock(Detection.class);
    when(detectionMock.getId()).thenReturn(detectionIdentifier);
    when(detectionMock.getCommunityOwnerId()).thenReturn(randomUUID().toString());
    when(detectionRepositoryMock.findById(detectionIdentifier))
        .thenReturn(Optional.of(detectionMock));
    when(communityAuthorizationRepositoryMock.findById(any())).thenReturn(Optional.empty());

    assertThrows(RuntimeException.class, () -> registerDetection(detectionIdentifier));
  }
}
