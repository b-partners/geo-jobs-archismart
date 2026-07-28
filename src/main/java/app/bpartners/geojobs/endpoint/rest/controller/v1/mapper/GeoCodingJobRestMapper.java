package app.bpartners.geojobs.endpoint.rest.controller.v1.mapper;

import static java.time.Instant.now;

import app.bpartners.geojobs.endpoint.rest.model.GeoCodingJob;
import app.bpartners.geojobs.endpoint.rest.model.Status;
import app.bpartners.geojobs.file.bucket.BucketComponent;
import app.bpartners.geojobs.repository.model.geocoding.GeoCodingJobStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class GeoCodingJobRestMapper {
  private final BucketComponent bucketComponent;

  public GeoCodingJob toRest(app.bpartners.geojobs.repository.model.geocoding.GeoCodingJob domain) {
    var status = domain.getStatus();
    var restStatus = mapToRest(status, domain.getMessage());
    return new GeoCodingJob()
        .id(domain.getEndToEndId())
        .geoJsonUrl(
            domain.getGeoJsonKey() == null ? null : bucketComponent.presign(domain.getGeoJsonKey()))
        .status(restStatus);
  }

  private Status mapToRest(GeoCodingJobStatus status, String message) {
    Status.ProgressionEnum progressionStatus;
    Status.HealthEnum healthStatus;
    if (status == null) {
      progressionStatus = null;
      healthStatus = null;
    } else {
      switch (status) {
        case PENDING -> {
          progressionStatus = Status.ProgressionEnum.PENDING;
          healthStatus = Status.HealthEnum.UNKNOWN;
        }
        case PROCESSING -> {
          progressionStatus = Status.ProgressionEnum.PROCESSING;
          healthStatus = Status.HealthEnum.UNKNOWN;
        }
        case SUCCEEDED -> {
          progressionStatus = Status.ProgressionEnum.FINISHED;
          healthStatus = Status.HealthEnum.SUCCEEDED;
        }
        case FAILED -> {
          progressionStatus = Status.ProgressionEnum.FINISHED;
          healthStatus = Status.HealthEnum.FAILED;
        }
        default -> throw new IllegalArgumentException("Unknown GeoCodingJob status: " + status);
      }
    }
    return status == null
        ? null
        : new Status()
            .progression(progressionStatus)
            .health(healthStatus)
            .message(message)
            .creationDatetime(now());
  }
}
