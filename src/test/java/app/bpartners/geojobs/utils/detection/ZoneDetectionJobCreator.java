package app.bpartners.geojobs.utils.detection;

import static app.bpartners.geojobs.repository.model.detection.ZoneDetectionJob.DetectionType.MACHINE;
import static java.time.Instant.now;
import static java.util.UUID.randomUUID;

import app.bpartners.geojobs.job.model.JobStatus;
import app.bpartners.geojobs.job.model.Status;
import app.bpartners.geojobs.repository.model.detection.ZoneDetectionJob;
import app.bpartners.geojobs.repository.model.tiling.ZoneTilingJob;
import java.util.ArrayList;

public class ZoneDetectionJobCreator {
  public ZoneDetectionJob create(
      String jobId,
      String zoneName,
      String emailReceiver,
      Status.ProgressionStatus progressionStatus,
      Status.HealthStatus healthStatus,
      ZoneTilingJob ztj) {
    var statuses = new ArrayList<JobStatus>();
    statuses.add(
        JobStatus.builder()
            .id(randomUUID().toString())
            .jobId(jobId)
            .progression(progressionStatus)
            .health(healthStatus)
            .creationDatetime(now())
            .build());
    return ZoneDetectionJob.builder()
        .id(jobId)
        .zoneName(zoneName)
        .emailReceiver(emailReceiver)
        .detectionType(MACHINE)
        .statusHistory(statuses)
        .zoneTilingJob(ztj)
        .submissionInstant(now())
        .build();
  }
}
