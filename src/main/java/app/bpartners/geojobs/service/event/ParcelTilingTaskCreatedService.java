package app.bpartners.geojobs.service.event;

import app.bpartners.geojobs.endpoint.event.model.tile.ParcelTilingTaskCreated;
import app.bpartners.geojobs.job.repository.TaskRepository;
import app.bpartners.geojobs.job.service.TaskStatusService;
import app.bpartners.geojobs.repository.model.tiling.ParcelTilingTask;
import app.bpartners.geojobs.service.TaskConsumer;
import org.springframework.stereotype.Service;

@Service
public class ParcelTilingTaskCreatedService
    extends TaskCreatedService<ParcelTilingTask, ParcelTilingTaskCreated> {

  public ParcelTilingTaskCreatedService(
      TaskConsumer<ParcelTilingTask> taskConsumer,
      TaskStatusService<ParcelTilingTask> taskStatusService,
      TaskRepository<ParcelTilingTask> taskRepository) {
    super(taskConsumer, taskStatusService, taskRepository);
  }

  @Override
  public void accept(ParcelTilingTaskCreated parcelTilingTaskCreated) {
    super.accept(parcelTilingTaskCreated);
  }
}
