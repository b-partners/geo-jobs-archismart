package app.bpartners.geojobs.utils.detection;

import static java.time.Instant.now;

import app.bpartners.geojobs.job.model.Status;
import app.bpartners.geojobs.job.model.TaskStatus;
import app.bpartners.geojobs.repository.model.Parcel;
import app.bpartners.geojobs.repository.model.detection.ParcelDetectionTask;
import java.util.ArrayList;
import java.util.List;

public class ParcelDetectionTaskCreator {
  public ParcelDetectionTask create(
      String taskId,
      String jobId,
      String asJobId,
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
    return ParcelDetectionTask.builder()
        .id(taskId)
        .jobId(jobId)
        .asJobId(asJobId)
        .submissionInstant(now())
        .parcels(List.of(parcel))
        .statusHistory(taskStatuses)
        .build();
  }
}
