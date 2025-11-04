package app.bpartners.geojobs.utils.tiling;

import static java.time.Instant.now;
import static java.util.UUID.randomUUID;

import app.bpartners.geojobs.job.model.JobStatus;
import app.bpartners.geojobs.job.model.Status;
import app.bpartners.geojobs.repository.model.tiling.ZoneTilingJob;
import java.util.List;

public class ZoneTilingJobCreator {
  public ZoneTilingJob create(
      String jobId,
      String zoneName,
      String emailReceiver,
      Status.ProgressionStatus progressionStatus,
      Status.HealthStatus healthStatus) {
    return ZoneTilingJob.builder()
        .id(jobId)
        .zoneName(zoneName)
        .emailReceiver(emailReceiver)
        .statusHistory(
            List.of(
                JobStatus.builder()
                    .id(randomUUID().toString())
                    .jobId(jobId)
                    .progression(progressionStatus)
                    .health(healthStatus)
                    .creationDatetime(now())
                    .build()))
        .submissionInstant(now())
        .build();
  }
}
