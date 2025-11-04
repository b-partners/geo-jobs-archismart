package app.bpartners.geojobs.service.event;

import app.bpartners.geojobs.endpoint.event.model.status.ZTJStatusRecomputingSubmitted;
import app.bpartners.geojobs.job.repository.TaskRepository;
import app.bpartners.geojobs.job.service.TaskStatusService;
import app.bpartners.geojobs.repository.model.tiling.ParcelTilingTask;
import app.bpartners.geojobs.repository.model.tiling.ZoneTilingJob;
import app.bpartners.geojobs.service.tiling.ZoneTilingJobService;
import java.util.function.Consumer;
import org.springframework.stereotype.Service;

@Service
public class ZTJStatusRecomputingSubmittedService
    implements Consumer<ZTJStatusRecomputingSubmitted> {
  private final JobStatusRecomputingSubmittedService<
          ZoneTilingJob, ParcelTilingTask, ZTJStatusRecomputingSubmitted>
      service;

  public ZTJStatusRecomputingSubmittedService(
      ZoneTilingJobService jobService,
      TaskStatusService<ParcelTilingTask> taskStatusService,
      TaskRepository<ParcelTilingTask> taskRepository) {
    this.service =
        new JobStatusRecomputingSubmittedService<>(jobService, taskStatusService, taskRepository);
  }

  @Override
  public void accept(ZTJStatusRecomputingSubmitted event) {
    service.accept(event);
  }
}
