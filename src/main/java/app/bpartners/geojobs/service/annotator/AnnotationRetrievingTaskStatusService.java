package app.bpartners.geojobs.service.annotator;

import app.bpartners.geojobs.job.repository.TaskStatusRepository;
import app.bpartners.geojobs.job.service.TaskStatusService;
import app.bpartners.geojobs.repository.model.annotation.AnnotationRetrievingTask;
import org.springframework.stereotype.Service;

@Service
public class AnnotationRetrievingTaskStatusService
    extends TaskStatusService<AnnotationRetrievingTask> {
  public AnnotationRetrievingTaskStatusService(TaskStatusRepository taskStatusRepository) {
    super(taskStatusRepository);
  }
}
