package app.bpartners.geojobs.service;

import static app.bpartners.geojobs.job.model.Status.HealthStatus.UNKNOWN;
import static app.bpartners.geojobs.job.model.Status.ProgressionStatus.PENDING;
import static app.bpartners.geojobs.repository.model.GeoJobType.DETECTION_ADDRESS_CONVERSION;
import static java.time.Instant.now;
import static java.util.UUID.randomUUID;

import app.bpartners.geojobs.job.model.JobStatus;
import app.bpartners.geojobs.repository.model.DetectionAddressConversionJob;
import app.bpartners.geojobs.repository.model.detection.Detection;
import org.springframework.stereotype.Component;

@Component
public class DetectionAddressConversionJobMapper {

  public DetectionAddressConversionJob fromDetection(Detection savedDetection) {
    var detectionAddressConversionJobId = randomUUID().toString();
    var detectionAddressConversionJob =
        app.bpartners.geojobs.repository.model.DetectionAddressConversionJob.builder()
            .id(detectionAddressConversionJobId)
            .detectionId(savedDetection.getId())
            .emailReceiver(savedDetection.getEmailReceiver())
            .zoneName(savedDetection.getZoneName())
            .submissionInstant(now())
            .build();
    detectionAddressConversionJob.hasNewStatus(
        JobStatus.builder()
            .jobId(detectionAddressConversionJobId)
            .id(randomUUID().toString())
            .creationDatetime(now())
            .jobType(DETECTION_ADDRESS_CONVERSION)
            .progression(PENDING)
            .health(UNKNOWN)
            .build());
    return detectionAddressConversionJob;
  }
}
