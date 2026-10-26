package app.bpartners.geojobs.service;

import static java.time.Instant.now;

import app.bpartners.geojobs.repository.CommunityAuthorizationRepository;
import app.bpartners.geojobs.repository.DetectionRepository;
import app.bpartners.geojobs.repository.model.community.CommunityAuthorization;
import app.bpartners.geojobs.repository.model.detection.Detection;
import app.bpartners.geojobs.service.dashboard.DetectionTrackingApi;
import app.bpartners.geojobs.service.dashboard.component.CreateDetectionTracking;
import app.bpartners.geojobs.service.dashboard.component.DetectionInitiator;
import java.util.List;
import java.util.function.Consumer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DetectionTrackingRegister implements Consumer<Detection> {
  private final DetectionTrackingApi detectionTrackingApi;
  private final CommunityAuthorizationRepository communityAuthorizationRepository;
  private final DetectionRepository detectionRepository;

  @Override
  public void accept(Detection detection) {
    if (detection.isDashboardRegistrationCompleted()) {
      log.info(
          "Detection {} already registered in dashboard {}",
          detection.getId(),
          detection.getDashboardRegistrationDatetime());
      return;
    }
    detectionTrackingApi.registerDetection(
        getApiKey(detection),
        List.of(
            new CreateDetectionTracking(
                detection.getZoneName(),
                "non supportée",
                now(),
                new DetectionInitiator(
                    "non supporté", detection.getEmailReceiver(), "non supporté"),
                detection.getEndToEndId())));

    var actualDetection = detectionRepository.findById(detection.getId()).orElseThrow();

    detectionRepository.save(
        actualDetection.toBuilder().dashboardRegistrationDatetime(now()).build());
  }

  private String getApiKey(Detection detection) {
    return communityAuthorizationRepository
        .findById(detection.getCommunityOwnerId())
        .map(CommunityAuthorization::getDashboardApiKey)
        .orElseThrow();
  }
}
