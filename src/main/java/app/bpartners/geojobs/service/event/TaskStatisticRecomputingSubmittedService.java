package app.bpartners.geojobs.service.event;

import app.bpartners.geojobs.endpoint.event.model.status.TaskStatisticRecomputingSubmitted;
import app.bpartners.geojobs.model.exception.NotFoundException;
import app.bpartners.geojobs.repository.*;
import app.bpartners.geojobs.repository.model.detection.ParcelDetectionTask;
import app.bpartners.geojobs.repository.model.detection.ZoneDetectionJob;
import app.bpartners.geojobs.repository.model.tiling.ParcelTilingTask;
import app.bpartners.geojobs.repository.model.tiling.ZoneTilingJob;
import java.util.function.Consumer;
import org.springframework.stereotype.Service;

@Service
public class TaskStatisticRecomputingSubmittedService
    implements Consumer<TaskStatisticRecomputingSubmitted> {

  private final TaskStatisticRecomputingSubmittedConsumer<ParcelTilingTask, ZoneTilingJob>
      tilingStatisticConsumer;
  private final TaskStatisticRecomputingSubmittedConsumer<ParcelDetectionTask, ZoneDetectionJob>
      zoneDetectionStatisticConsumer;

  public TaskStatisticRecomputingSubmittedService(
      ZoneDetectionJobRepository zoneDetectionJobRepository,
      ParcelDetectionTaskRepository parcelDetectionTaskRepository,
      ZoneTilingJobRepository zoneTilingJobRepository,
      TilingTaskRepository tilingTaskRepository,
      TaskStatisticRepository taskStatisticRepository) {
    this.zoneDetectionStatisticConsumer =
        new TaskStatisticRecomputingSubmittedConsumer<>(
            zoneDetectionJobRepository, parcelDetectionTaskRepository, taskStatisticRepository);
    this.tilingStatisticConsumer =
        new TaskStatisticRecomputingSubmittedConsumer<>(
            zoneTilingJobRepository, tilingTaskRepository, taskStatisticRepository);
  }

  @Override
  public void accept(TaskStatisticRecomputingSubmitted taskStatisticRecomputingSubmitted) {
    try {
      tilingStatisticConsumer.accept(taskStatisticRecomputingSubmitted);
    } catch (NotFoundException e) {
      zoneDetectionStatisticConsumer.accept(taskStatisticRecomputingSubmitted);
    }
  }
}
