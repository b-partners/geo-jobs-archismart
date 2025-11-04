package app.bpartners.geojobs.service;

import static app.bpartners.geojobs.job.model.Status.HealthStatus.RETRYING;
import static app.bpartners.geojobs.job.model.Status.ProgressionStatus.PENDING;
import static app.bpartners.geojobs.repository.model.GeoJobType.DETECTION;
import static app.bpartners.geojobs.repository.model.GeoJobType.TILING;
import static java.time.Instant.now;
import static java.util.UUID.randomUUID;

import app.bpartners.geojobs.job.model.JobType;
import app.bpartners.geojobs.job.model.Task;
import app.bpartners.geojobs.job.model.TaskStatus;
import app.bpartners.geojobs.repository.model.TileDetectionTask;
import app.bpartners.geojobs.repository.model.detection.ParcelDetectionTask;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import org.springframework.stereotype.Component;

@Component
public class NotFinishedTaskRetriever<T extends Task> implements Function<T, T> {
  @Override
  public T apply(T failedTask) {
    JobType jobType =
        failedTask instanceof ParcelDetectionTask || failedTask instanceof TileDetectionTask
            ? DETECTION
            : TILING;
    List<TaskStatus> newStatus = new ArrayList<>(failedTask.getStatusHistory());
    newStatus.add(
        TaskStatus.builder()
            .id(randomUUID().toString())
            .taskId(failedTask.getId())
            .jobType(jobType)
            .progression(PENDING)
            .health(RETRYING)
            .creationDatetime(now())
            .build());
    failedTask.setStatusHistory(newStatus);
    return failedTask;
  }
}
