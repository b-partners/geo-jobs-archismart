package app.bpartners.geojobs.utils.detection;

import static java.time.Instant.now;

import app.bpartners.geojobs.job.model.Status;
import app.bpartners.geojobs.job.model.TaskStatus;
import app.bpartners.geojobs.repository.model.TileDetectionTask;
import app.bpartners.geojobs.repository.model.tiling.Tile;
import java.util.ArrayList;
import java.util.List;

public class TileDetectionTaskCreator {
  public TileDetectionTask create(
      String id,
      String jobId,
      String parcelId,
      Tile tile,
      Status.ProgressionStatus progressionStatus,
      Status.HealthStatus healthStatus) {
    List<TaskStatus> taskStatuses = new ArrayList<>();
    taskStatuses.add(
        TaskStatus.builder()
            .progression(progressionStatus)
            .health(healthStatus)
            .creationDatetime(now())
            .taskId(id)
            .build());

    return TileDetectionTask.builder()
        .id(id)
        .jobId(jobId)
        .parcelId(parcelId)
        .tile(tile)
        .statusHistory(taskStatuses)
        .submissionInstant(now())
        .build();
  }
}
