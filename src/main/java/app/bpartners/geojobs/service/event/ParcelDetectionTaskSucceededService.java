package app.bpartners.geojobs.service.event;

import app.bpartners.geojobs.endpoint.event.model.parcel.ParcelDetectionTaskSucceeded;
import app.bpartners.geojobs.job.service.TaskStatusService;
import app.bpartners.geojobs.repository.ParcelDetectionTaskRepository;
import app.bpartners.geojobs.repository.model.detection.ParcelDetectionTask;
import java.util.function.Consumer;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@AllArgsConstructor
@Service
public class ParcelDetectionTaskSucceededService implements Consumer<ParcelDetectionTaskSucceeded> {
  private final ParcelDetectionTaskRepository taskRepository;
  private final TaskStatusService<ParcelDetectionTask> taskStatusService;

  @Override
  public void accept(ParcelDetectionTaskSucceeded parcelDetectionTaskSucceeded) {
    var task = parcelDetectionTaskSucceeded.getTask();
    taskRepository.save(task);
    taskStatusService.succeed(task);
  }
}
