package app.bpartners.geojobs.service.event;

import app.bpartners.geojobs.endpoint.event.model.DetectionCreated;
import app.bpartners.geojobs.service.detection.DetectionTilingCreation;
import java.util.function.Consumer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DetectionCreatedService implements Consumer<DetectionCreated> {
  private final DetectionTilingCreation detectionTilingCreation;

  @Override
  @Transactional
  public void accept(DetectionCreated detectionCreated) {
    detectionTilingCreation.apply(detectionCreated.getDetection());
  }
}
