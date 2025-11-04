package app.bpartners.geojobs.service.event;

import app.bpartners.geojobs.endpoint.event.model.DetectionAddressConversionTaskSucceeded;
import app.bpartners.geojobs.repository.DetectionAddressConversionTaskRepository;
import app.bpartners.geojobs.service.DetectionAddressConversionTaskStatusService;
import java.util.function.Consumer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DetectionAddressConversionTaskSucceededService
    implements Consumer<DetectionAddressConversionTaskSucceeded> {
  private final DetectionAddressConversionTaskStatusService taskStatusService;
  private final DetectionAddressConversionTaskRepository taskRepository;

  @Override
  public void accept(DetectionAddressConversionTaskSucceeded event) {
    var task = event.getSucceededTask();
    taskRepository.save(task);
    taskStatusService.succeed(task);
  }
}
