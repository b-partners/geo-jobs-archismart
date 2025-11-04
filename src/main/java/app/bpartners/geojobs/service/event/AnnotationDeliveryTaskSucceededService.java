package app.bpartners.geojobs.service.event;

import app.bpartners.geojobs.endpoint.event.model.annotation.AnnotationDeliveryTaskSucceeded;
import app.bpartners.geojobs.job.service.TaskStatusService;
import app.bpartners.geojobs.repository.AnnotationDeliveryTaskRepository;
import app.bpartners.geojobs.repository.model.annotation.AnnotationDeliveryTask;
import java.util.function.Consumer;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class AnnotationDeliveryTaskSucceededService
    implements Consumer<AnnotationDeliveryTaskSucceeded> {
  private final TaskStatusService<AnnotationDeliveryTask> taskStatusService;
  private final AnnotationDeliveryTaskRepository repository;

  @Override
  public void accept(AnnotationDeliveryTaskSucceeded event) {
    var task = event.getDeliveryTask();
    repository.save(task);
    taskStatusService.succeed(task);
  }
}
