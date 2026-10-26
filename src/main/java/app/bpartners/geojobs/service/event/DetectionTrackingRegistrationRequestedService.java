package app.bpartners.geojobs.service.event;

import app.bpartners.geojobs.endpoint.event.model.DetectionTrackingRegistrationRequested;
import app.bpartners.geojobs.model.exception.NotFoundException;
import app.bpartners.geojobs.repository.DetectionRepository;
import app.bpartners.geojobs.service.DetectionTrackingRegister;
import java.util.function.Consumer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DetectionTrackingRegistrationRequestedService
    implements Consumer<DetectionTrackingRegistrationRequested> {
  private final DetectionTrackingRegister detectionTrackingRegister;
  private final DetectionRepository detectionRepository;

  @Override
  public void accept(DetectionTrackingRegistrationRequested event) {
    var detectionIdentifier = event.getDetectionIdentifier();
    var detection =
        detectionRepository
            .findById(detectionIdentifier)
            .orElseThrow(
                () -> new NotFoundException("Detection not found for id=" + detectionIdentifier));

    detectionTrackingRegister.accept(detection);
  }
}
