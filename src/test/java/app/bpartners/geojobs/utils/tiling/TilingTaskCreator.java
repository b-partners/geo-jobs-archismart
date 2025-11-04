package app.bpartners.geojobs.utils.tiling;

import static java.time.Instant.now;

import app.bpartners.geojobs.job.model.Status;
import app.bpartners.geojobs.job.model.TaskStatus;
import app.bpartners.geojobs.repository.model.Parcel;
import app.bpartners.geojobs.repository.model.tiling.ParcelTilingTask;
import java.util.ArrayList;
import java.util.List;

public class TilingTaskCreator {
  public ParcelTilingTask create(
      String taskId,
      String jobId,
      Parcel parcel,
      Status.ProgressionStatus progressionStatus,
      Status.HealthStatus healthStatus) {
    List<TaskStatus> taskStatuses = new ArrayList<>();
    taskStatuses.add(
        TaskStatus.builder()
            .progression(progressionStatus)
            .health(healthStatus)
            .creationDatetime(now())
            .taskId(taskId)
            .build());
    return ParcelTilingTask.builder()
        .id(taskId)
        .jobId(jobId)
        .submissionInstant(now())
        .parcels(List.of(parcel))
        .statusHistory(taskStatuses)
        .build();
  }
}
