package app.bpartners.geojobs.service;

import static app.bpartners.geojobs.repository.model.dashboard.DashboardRegistrationType.DETECTION_2D;
import static java.time.Instant.now;

import app.bpartners.geojobs.model.exception.DashboardUnavailableException;
import app.bpartners.geojobs.repository.CommunityAuthorizationRepository;
import app.bpartners.geojobs.repository.DetectionRepository;
import app.bpartners.geojobs.repository.model.detection.Detection;
import app.bpartners.geojobs.service.dashboard.AnalysisProjectApi;
import app.bpartners.geojobs.service.dashboard.AnalysisProjectBilling;
import app.bpartners.geojobs.service.dashboard.AnalysisProjectKeys;
import app.bpartners.geojobs.service.dashboard.component.DetectionInitiator;
import app.bpartners.geojobs.service.dashboard.component.Register2d;
import java.util.function.BiConsumer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DetectionTrackingRegister implements BiConsumer<Detection, String> {
  private final AnalysisProjectApi analysisProjectApi;
  private final AnalysisProjectBilling analysisProjectBilling;
  private final DashboardRegistrationFailureService dashboardRegistrationFailureService;
  private final CommunityAuthorizationRepository communityAuthorizationRepository;
  private final DetectionRepository detectionRepository;
  private final DetectionAddressResolver detectionAddressResolver;

  @Override
  public void accept(Detection detection, String failureId) {
    if (detection.isDashboardRegistrationCompleted()) {
      log.info(
          "Detection {} already registered in dashboard {}",
          detection.getId(),
          detection.getDashboardRegistrationDatetime());
      resolveFailure(failureId);
      return;
    }
    var community =
        communityAuthorizationRepository.findById(detection.getCommunityOwnerId()).orElseThrow();
    var projectKey = projectKeyOf(detection);
    var register2d =
        new Register2d(
            detection.getEndToEndId(),
            detection.getZoneName(),
            detectionAddressResolver.apply(detection),
            now(),
            new DetectionInitiator(community.getName(), detection.getEmailReceiver(), null),
            detection.getExternalReference());
    boolean registered;
    try {
      registered =
          analysisProjectBilling.register(
              community,
              "Detection " + detection.getId(),
              apiKey -> analysisProjectApi.register2d(apiKey, projectKey, register2d));
    } catch (DashboardUnavailableException e) {
      dashboardRegistrationFailureService.recordFailure(
          DETECTION_2D,
          detection.getId(),
          detection.getCommunityOwnerId(),
          projectKey,
          register2d,
          e);
      return;
    }
    resolveFailure(failureId);
    if (!registered) {
      return;
    }

    var actualDetection = detectionRepository.findById(detection.getId()).orElseThrow();

    detectionRepository.save(
        actualDetection.toBuilder().dashboardRegistrationDatetime(now()).build());
  }

  private void resolveFailure(String failureId) {
    if (failureId != null) {
      dashboardRegistrationFailureService.resolve(failureId);
    }
  }

  public static boolean isIndividual(Detection detection) {
    var convertedAddresses = detection.getConvertedAddresses();
    if (convertedAddresses != null && convertedAddresses.size() > 1) {
      return false;
    }
    return AnalysisProjectKeys.isIndividual(detection.getDomainProvidedGeoJsonZone());
  }

  public static String projectKeyOf(Detection detection) {
    var providedKey = AnalysisProjectKeys.providedOrNull(detection.getProjectKey());
    if (providedKey != null) {
      return providedKey;
    }
    var detectionKey = "detection:" + detection.getId();
    if (!isIndividual(detection)) {
      return detectionKey;
    }
    return AnalysisProjectKeys.of(detection.getDomainProvidedGeoJsonZone()).orElse(detectionKey);
  }
}
