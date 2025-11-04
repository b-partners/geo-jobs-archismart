package app.bpartners.geojobs.service.event;

import app.bpartners.geojobs.endpoint.event.model.status.ParcelDetectionStatusRecomputingSubmitted;
import app.bpartners.geojobs.repository.TileDetectionTaskRepository;
import app.bpartners.geojobs.repository.model.TileDetectionTask;
import app.bpartners.geojobs.repository.model.detection.ParcelDetectionJob;
import app.bpartners.geojobs.service.detection.ParcelDetectionJobService;
import app.bpartners.geojobs.service.detection.TileDetectionTaskStatusService;
import java.util.function.Consumer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
// WARNING: Do not change into inheritance as ServiceInvoker will not find the accept method
public class ParcelDetectionStatusRecomputingSubmittedService
    implements Consumer<ParcelDetectionStatusRecomputingSubmitted> {
  private final JobStatusRecomputingSubmittedService<
          ParcelDetectionJob, TileDetectionTask, ParcelDetectionStatusRecomputingSubmitted>
      service;

  public ParcelDetectionStatusRecomputingSubmittedService(
      ParcelDetectionJobService jobService,
      TileDetectionTaskStatusService taskStatusService,
      TileDetectionTaskRepository taskRepository) {
    this.service =
        new JobStatusRecomputingSubmittedService<>(jobService, taskStatusService, taskRepository);
  }

  @Override
  public void accept(ParcelDetectionStatusRecomputingSubmitted event) {
    service.accept(event);
  }
}
