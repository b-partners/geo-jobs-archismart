package app.bpartners.geojobs.endpoint.rest.validator;

import app.bpartners.geojobs.job.model.Status;
import app.bpartners.geojobs.model.exception.NotFoundException;
import app.bpartners.geojobs.model.exception.NotImplementedException;
import app.bpartners.geojobs.repository.ZoneDetectionJobRepository;
import app.bpartners.geojobs.repository.model.detection.ZoneDetectionJob;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class ZoneDetectionJobValidator {
  private final ZoneDetectionJobRepository jobRepository;

  public void accept(String jobId) {
    var optionalZdj = jobRepository.findById(jobId);
    if (optionalZdj.isEmpty()) {
      throw new NotFoundException("ZoneDetectionJob(id=" + jobId + ") not found");
    }
    var zoneDetectionJob = optionalZdj.get();
    if (zoneDetectionJob.getDetectionType() == ZoneDetectionJob.DetectionType.HUMAN) {
      throw new NotImplementedException(
          "Only MACHINE detection type can be processed manually for now");
    }
    if (zoneDetectionJob.getStatus().getProgression() != Status.ProgressionStatus.PENDING) {
      throw new NotImplementedException("Only PENDING ZoneDetectionJob can be processed for now");
    }
  }
}
