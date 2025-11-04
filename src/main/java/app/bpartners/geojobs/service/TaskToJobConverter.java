package app.bpartners.geojobs.service;

import static app.bpartners.geojobs.job.model.Status.HealthStatus.UNKNOWN;
import static app.bpartners.geojobs.job.model.Status.ProgressionStatus.PENDING;
import static app.bpartners.geojobs.repository.model.GeoJobType.PARCEL_DETECTION;
import static java.time.Instant.now;
import static java.util.UUID.randomUUID;

import app.bpartners.geojobs.job.model.Job;
import app.bpartners.geojobs.job.model.JobStatus;
import app.bpartners.geojobs.job.model.Task;
import app.bpartners.geojobs.job.model.TaskStatus;
import app.bpartners.geojobs.model.exception.NotImplementedException;
import app.bpartners.geojobs.repository.model.TileDetectionTask;
import app.bpartners.geojobs.repository.model.detection.ParcelDetectionJob;
import app.bpartners.geojobs.repository.model.detection.ParcelDetectionTask;
import app.bpartners.geojobs.repository.model.tiling.Tile;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import lombok.NonNull;
import org.springframework.stereotype.Component;

@Component
public class TaskToJobConverter<T extends Task, J extends Job> implements Function<T, J> {

  @Override
  public J apply(T task) {
    if (task instanceof ParcelDetectionTask) {
      String jobId = task.getAsJobId() == null ? randomUUID().toString() : task.getAsJobId();
      return (J)
          ParcelDetectionJob.builder()
              .id(jobId)
              .address(((ParcelDetectionTask) task).getAddress())
              .point(((ParcelDetectionTask) task).getPoint())
              .statusHistory(
                  List.of(
                      JobStatus.builder()
                          .jobId(jobId)
                          .id(randomUUID().toString())
                          .creationDatetime(now())
                          .jobType(PARCEL_DETECTION)
                          .progression(PENDING)
                          .health(UNKNOWN)
                          .build()))
              .submissionInstant(now())
              .build();
    }
    throw new NotImplementedException(
        "Only ParcelDetectionTask to ParcelDetectionJob is handle for now");
  }

  @NonNull
  public TileDetectionTask apply(ParcelDetectionTask task, Tile tile) {
    String tileDetectionTaskId = randomUUID().toString();
    String parcelId = task.getParcel().getId();
    String jobId = task.getAsJobId();
    List<TaskStatus> status = new ArrayList<>();
    status.add(
        TaskStatus.builder()
            .health(UNKNOWN)
            .progression(PENDING)
            .creationDatetime(now())
            .taskId(tileDetectionTaskId)
            .build());
    return new TileDetectionTask(tileDetectionTaskId, null, parcelId, jobId, tile, status);
  }
}
