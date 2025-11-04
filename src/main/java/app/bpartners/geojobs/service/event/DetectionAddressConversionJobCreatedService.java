package app.bpartners.geojobs.service.event;

import app.bpartners.geojobs.endpoint.event.model.zone.DetectionAddressConversionJobCreated;
import app.bpartners.geojobs.service.DetectionAddressConversionJobService;
import java.util.function.Consumer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DetectionAddressConversionJobCreatedService
    implements Consumer<DetectionAddressConversionJobCreated> {
  private final DetectionAddressConversionJobService detectionAddressConversionJobService;

  @Override
  public void accept(DetectionAddressConversionJobCreated event) {
    var detectionAddressConversionJob = event.getJob();

    detectionAddressConversionJobService.fireTasks(detectionAddressConversionJob.getId());
  }
}
