package app.bpartners.geojobs.service;

import static app.bpartners.geojobs.repository.model.cityjson.CityJSONDelimitationObjectType.BUILDING_ROOF_SEGMENT_FACE;
import static app.bpartners.geojobs.repository.model.dashboard.DashboardRegistrationType.THREE_D_3D;
import static java.time.Instant.now;

import app.bpartners.geojobs.model.exception.DashboardUnavailableException;
import app.bpartners.geojobs.model.exception.NotFoundException;
import app.bpartners.geojobs.repository.CityJSONRequestRepository;
import app.bpartners.geojobs.repository.CommunityAuthorizationRepository;
import app.bpartners.geojobs.repository.model.cityjson.CityJSONRequest;
import app.bpartners.geojobs.service.dashboard.AnalysisProjectApi;
import app.bpartners.geojobs.service.dashboard.AnalysisProjectBilling;
import app.bpartners.geojobs.service.dashboard.AnalysisProjectKeys;
import app.bpartners.geojobs.service.dashboard.component.Register3d;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ThreeDTrackingRegister {
  private final AnalysisProjectApi analysisProjectApi;
  private final AnalysisProjectBilling analysisProjectBilling;
  private final DashboardRegistrationFailureService dashboardRegistrationFailureService;
  private final CommunityAuthorizationRepository communityAuthorizationRepository;
  private final CityJSONRequestRepository cityJSONRequestRepository;

  public void accept(String requestId, String communityOwnerId, String failureId) {
    var request =
        cityJSONRequestRepository
            .findByIdAndCommunityOwnerId(requestId, communityOwnerId)
            .orElseThrow(() -> new NotFoundException("3D request not found for id=" + requestId));
    if (request.isDashboardRegistrationCompleted()) {
      log.info(
          "3D request {} already registered in dashboard {}",
          request.getId(),
          request.getDashboardRegistrationDatetime());
      resolveFailure(failureId);
      return;
    }
    var community = communityAuthorizationRepository.findById(communityOwnerId).orElseThrow();
    var projectKey = projectKeyOf(request);
    var register3d = new Register3d(null, request.getExternalReference());
    boolean registered;
    try {
      registered =
          analysisProjectBilling.register(
              community,
              "3D request " + request.getId(),
              apiKey -> analysisProjectApi.register3d(apiKey, projectKey, register3d));
    } catch (DashboardUnavailableException e) {
      dashboardRegistrationFailureService.recordFailure(
          THREE_D_3D, request.getId(), communityOwnerId, projectKey, register3d, e);
      return;
    }
    resolveFailure(failureId);
    if (registered) {
      cityJSONRequestRepository.markDashboardRegistered(request.getId(), now());
    }
  }

  private void resolveFailure(String failureId) {
    if (failureId != null) {
      dashboardRegistrationFailureService.resolve(failureId);
    }
  }

  public static boolean isIndividual(CityJSONRequest request) {
    var delimitations = request.getRequestDelimitations();
    if (BUILDING_ROOF_SEGMENT_FACE.equals(request.getDelimitationObjectType())) {
      return AnalysisProjectKeys.hasGeometry(delimitations);
    }
    return AnalysisProjectKeys.isIndividual(delimitations);
  }

  public static String projectKeyOf(CityJSONRequest request) {
    var providedKey = AnalysisProjectKeys.providedOrNull(request.getProjectKey());
    if (providedKey != null) {
      return providedKey;
    }
    var requestKey = "city-json:" + request.getId();
    if (!isIndividual(request)) {
      return requestKey;
    }
    return AnalysisProjectKeys.of(request.getRequestDelimitations()).orElse(requestKey);
  }
}
