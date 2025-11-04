package app.bpartners.geojobs.service.detection;

import app.bpartners.geojobs.job.repository.TaskStatusRepository;
import app.bpartners.geojobs.job.service.TaskStatusService;
import app.bpartners.geojobs.repository.model.TileDetectionTask;
import org.springframework.stereotype.Service;

@Service
public class TileDetectionTaskStatusService extends TaskStatusService<TileDetectionTask> {
  public TileDetectionTaskStatusService(TaskStatusRepository taskStatusRepository) {
    super(taskStatusRepository);
  }
}
