package app.bpartners.geojobs.service;

import static app.bpartners.geojobs.job.model.Status.HealthStatus.UNKNOWN;
import static app.bpartners.geojobs.job.model.Status.ProgressionStatus.PENDING;
import static java.time.Instant.now;
import static java.util.UUID.randomUUID;

import app.bpartners.geojobs.job.model.TaskStatus;
import app.bpartners.geojobs.repository.model.DetectionAddressConversionTask;
import app.bpartners.geojobs.repository.model.detection.Detection;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class DetectionAddressConversionTaskMapper {

  public List<DetectionAddressConversionTask> fromAddressListAndJobId(
      Detection detection, String detectionAddressConversionJobId) {
    return detection.getConvertedAddresses().stream()
        .map(address -> fromAddressAndJobId(address, detectionAddressConversionJobId))
        .toList();
  }

  private DetectionAddressConversionTask fromAddressAndJobId(String address, String jobId) {
    var generatedTaskId = randomUUID().toString();
    return DetectionAddressConversionTask.builder()
        .id(generatedTaskId)
        .jobId(jobId)
        .address(address)
        .feature(null) // Not computed yet here
        .statusHistory(
            List.of(
                TaskStatus.builder()
                    .id(randomUUID().toString())
                    .taskId(generatedTaskId)
                    .health(UNKNOWN)
                    .progression(PENDING)
                    .creationDatetime(now())
                    .build()))
        .submissionInstant(now())
        .build();
  }
}
