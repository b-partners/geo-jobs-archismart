package app.bpartners.geojobs.job.service;

import app.bpartners.geojobs.endpoint.rest.model.AnnotationJobProcessing;
import app.bpartners.geojobs.endpoint.rest.model.JobType;
import app.bpartners.geojobs.model.exception.NotImplementedException;
import app.bpartners.geojobs.repository.ZoneDetectionJobRepository;
import app.bpartners.geojobs.repository.ZoneTilingJobRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class JobAnnotationService {
  private final ZoneDetectionJobRepository zoneDetectionJobRepository;
  private final ZoneTilingJobRepository tilingJobRepository;
  private final AnnotationDetectionJobProcessing annotationDetectionJobProcessing;

  public AnnotationJobProcessing processAnnotationJob(
      String jobId, Double minConfidenceForDelivery) {
    var jobType = getJobType(jobId);
    if (JobType.DETECTION.equals(jobType)) {
      return annotationDetectionJobProcessing.apply(jobId, minConfidenceForDelivery);
    }
    throw new NotImplementedException(
        "Only DETECTION JobType is supported for annotation job processing");
  }

  private JobType getJobType(String jobId) {
    if (zoneDetectionJobRepository.findById(jobId).isPresent()) {
      return JobType.DETECTION;
    } else if (tilingJobRepository.findById(jobId).isPresent()) {
      return JobType.TILING;
    } else {
      throw new NotImplementedException("Unable to retrieve jobType for Job(id=" + jobId + ")");
    }
  }
}
