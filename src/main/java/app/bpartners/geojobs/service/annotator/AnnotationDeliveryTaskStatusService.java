package app.bpartners.geojobs.service.annotator;

import app.bpartners.geojobs.job.repository.TaskStatusRepository;
import app.bpartners.geojobs.job.service.TaskStatusService;
import app.bpartners.geojobs.repository.model.annotation.AnnotationDeliveryTask;
import org.springframework.stereotype.Service;

@Service
public class AnnotationDeliveryTaskStatusService extends TaskStatusService<AnnotationDeliveryTask> {
  public AnnotationDeliveryTaskStatusService(TaskStatusRepository taskStatusRepository) {
    super(taskStatusRepository);
  }
}
