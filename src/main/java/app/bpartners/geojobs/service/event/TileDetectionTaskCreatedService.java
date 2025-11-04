package app.bpartners.geojobs.service.event;

import app.bpartners.geojobs.endpoint.event.model.tile.TileDetectionTaskCreated;
import app.bpartners.geojobs.job.repository.TaskRepository;
import app.bpartners.geojobs.job.service.TaskStatusService;
import app.bpartners.geojobs.repository.model.TileDetectionTask;
import app.bpartners.geojobs.service.TaskConsumer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class TileDetectionTaskCreatedService
    extends TaskCreatedService<TileDetectionTask, TileDetectionTaskCreated> {

  public TileDetectionTaskCreatedService(
      TaskConsumer<TileDetectionTask> taskConsumer,
      TaskStatusService<TileDetectionTask> taskStatusService,
      TaskRepository<TileDetectionTask> taskRepository) {
    super(taskConsumer, taskStatusService, taskRepository);
  }

  @Override
  public void accept(TileDetectionTaskCreated event) {
    event.getTask().setDetectableObjectConfigurations(event.getDetectableObjectConfigurations());
    event.getTask().setZoneDetectionJobId(event.getZoneDetectionJobId());
    event.getTask().setAddress(event.getAddress());
    event.getTask().setPoint(event.getPoint());
    super.accept(event);
  }
}
