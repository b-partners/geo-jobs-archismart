package app.bpartners.geojobs.service;

import app.bpartners.geojobs.endpoint.event.EventProducer;
import app.bpartners.geojobs.endpoint.event.model.zone.DetectionAddressConversionJobCreated;
import app.bpartners.geojobs.repository.model.detection.Detection;
import java.util.List;
import java.util.function.Consumer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DetectionAddressConsumer implements Consumer<Detection> {
  private final DetectionAddressConversionJobMapper detectionAddressConversionJobMapper;
  private final DetectionAddressConversionJobService detectionAddressConversionJobService;
  private final EventProducer eventProducer;
  private final DetectionAddressConversionTaskMapper detectionAddressConversionTaskMapper;

  @Override
  public void accept(Detection detection) {
    var detectionAddressConversionJob =
        detectionAddressConversionJobMapper.fromDetection(detection);
    var detectionAddressConversionTasks =
        detectionAddressConversionTaskMapper.fromAddressListAndJobId(
            detection, detectionAddressConversionJob.getId());

    var savedAddressConversionJob =
        detectionAddressConversionJobService.create(
            detectionAddressConversionJob, detectionAddressConversionTasks);

    eventProducer.accept(
        List.of(
            DetectionAddressConversionJobCreated.builder().job(savedAddressConversionJob).build()));
  }
}
