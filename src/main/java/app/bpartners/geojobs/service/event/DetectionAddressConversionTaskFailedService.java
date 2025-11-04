package app.bpartners.geojobs.service.event;

import app.bpartners.geojobs.endpoint.event.model.DetectionAddressConversionTaskFailed;
import app.bpartners.geojobs.repository.DetectionAddressConversionTaskRepository;
import app.bpartners.geojobs.service.DetectionAddressConversionTaskStatusService;
import java.util.function.Consumer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DetectionAddressConversionTaskFailedService
    implements Consumer<DetectionAddressConversionTaskFailed> {
  private final DetectionAddressConversionTaskStatusService taskStatusService;
  private final DetectionAddressConversionTaskRepository taskRepository;

  @Override
  public void accept(DetectionAddressConversionTaskFailed event) {
    var task = event.getTask();
    taskRepository.save(task);
    taskStatusService.fail(task);
  }
}
