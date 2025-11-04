package app.bpartners.geojobs.service.tiling;

import app.bpartners.geojobs.job.repository.TaskStatusRepository;
import app.bpartners.geojobs.job.service.TaskStatusService;
import app.bpartners.geojobs.repository.model.tiling.ParcelTilingTask;
import org.springframework.stereotype.Service;

@Service
public class TilingTaskStatusService extends TaskStatusService<ParcelTilingTask> {

  public TilingTaskStatusService(TaskStatusRepository taskStatusRepository) {
    super(taskStatusRepository);
  }
}
