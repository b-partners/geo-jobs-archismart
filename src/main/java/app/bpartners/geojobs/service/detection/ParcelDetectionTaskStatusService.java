package app.bpartners.geojobs.service.detection;

import app.bpartners.geojobs.job.repository.TaskStatusRepository;
import app.bpartners.geojobs.job.service.TaskStatusService;
import app.bpartners.geojobs.repository.model.detection.ParcelDetectionTask;
import org.springframework.stereotype.Service;

@Service
public class ParcelDetectionTaskStatusService extends TaskStatusService<ParcelDetectionTask> {

  public ParcelDetectionTaskStatusService(TaskStatusRepository taskStatusRepository) {
    super(taskStatusRepository);
  }
}
