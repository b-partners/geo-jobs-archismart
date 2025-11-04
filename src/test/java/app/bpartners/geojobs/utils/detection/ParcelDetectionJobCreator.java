package app.bpartners.geojobs.utils.detection;

import static app.bpartners.geojobs.repository.model.GeoJobType.PARCEL_DETECTION;
import static java.time.Instant.now;
import static java.util.UUID.randomUUID;

import app.bpartners.geojobs.job.model.JobStatus;
import app.bpartners.geojobs.job.model.Status;
import app.bpartners.geojobs.repository.model.detection.ParcelDetectionJob;
import java.util.List;

public class ParcelDetectionJobCreator {

  public ParcelDetectionJob create(
      String parcelDetectionJobId,
      Status.ProgressionStatus progressionStatus,
      Status.HealthStatus healthStatus) {
    return ParcelDetectionJob.builder()
        .id(parcelDetectionJobId)
        .statusHistory(
            List.of(
                JobStatus.builder()
                    .jobId(parcelDetectionJobId)
                    .id(randomUUID().toString())
                    .creationDatetime(now())
                    .jobType(PARCEL_DETECTION)
                    .progression(progressionStatus)
                    .health(healthStatus)
                    .build()))
        .build();
  }
}
