package app.bpartners.geojobs.service.event;

import static java.time.Instant.now;

import app.bpartners.geojobs.endpoint.event.model.GeoJsonConversionProcessSucceeded;
import app.bpartners.geojobs.model.exception.NotFoundException;
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
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GeoJsonConversionProcessSucceededService
    implements Consumer<GeoJsonConversionProcessSucceeded> {
  private final DetectionTrackingApi detectionTrackingApi;
  private final CommunityAuthorizationRepository communityAuthorizationRepository;
  private final DetectionRepository detectionRepository;

  @Override
  public void accept(GeoJsonConversionProcessSucceeded event) {
    var detectionIdentifier = event.getDetectionIdentifier();
    var detection =
        detectionRepository
            .findById(detectionIdentifier)
            .orElseThrow(
                () -> new NotFoundException("Detection not found for id=" + detectionIdentifier));

    detectionTrackingApi.registerDetection(
        getApiKey(detection),
        List.of(
            new CreateDetectionTracking(
                detection.getZoneName(),
                "non supportée",
                now(),
                new DetectionInitiator(
                    "non supporté", detection.getEmailReceiver(), "non supporté"))));
  }

  private String getApiKey(Detection detection) {
    return communityAuthorizationRepository
        .findById(detection.getCommunityOwnerId())
        .map(CommunityAuthorization::getDashboardApiKey)
        .orElseThrow();
  }
}
