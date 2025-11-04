package app.bpartners.geojobs.service.event;

import static app.bpartners.geojobs.job.model.Status.HealthStatus.SUCCEEDED;
import static app.bpartners.geojobs.job.model.Status.ProgressionStatus.FINISHED;
import static app.bpartners.geojobs.repository.model.GeoJobType.DETECTION;
import static java.time.Instant.now;
import static java.util.UUID.randomUUID;

import app.bpartners.geojobs.endpoint.event.model.status.HumanZDJStatusRecomputingSubmitted;
import app.bpartners.geojobs.job.model.Job;
import app.bpartners.geojobs.job.model.JobStatus;
import app.bpartners.geojobs.job.model.Status;
import app.bpartners.geojobs.job.repository.JobStatusRepository;
import app.bpartners.geojobs.repository.model.detection.ZoneDetectionJob;
import app.bpartners.geojobs.service.AnnotationRetrievingJobService;
import app.bpartners.geojobs.service.detection.ZoneDetectionJobService;
import app.bpartners.geojobs.service.geojson.GeoJsonConversionJobService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.function.Consumer;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class HumanZDJStatusRecomputingSubmittedService
    implements Consumer<HumanZDJStatusRecomputingSubmitted> {
  private final ZoneDetectionJobService zoneDetectionJobService;
  private final AnnotationRetrievingJobService retrievingJobService;
  private final JobStatusRepository jobStatusRepository;
  private final GeoJsonConversionJobService geoJsonConversionJobService;
  @PersistenceContext EntityManager em;

  @Override
  public void accept(HumanZDJStatusRecomputingSubmitted event) {
    var detectionJobId = event.getJobId();
    var oldHumanZDJ = zoneDetectionJobService.findById(detectionJobId);
    em.detach(oldHumanZDJ);
    var oldHumanZDJStatus = oldHumanZDJ.getStatus();

    var newZDJ = (ZoneDetectionJob) oldHumanZDJ.semanticClone();
    var linkedRetrievingJobs = retrievingJobService.findAllByDetectionJobId(detectionJobId);
    linkedRetrievingJobs.forEach(em::detach);
    boolean linkedJobsAreSucceeded = linkedRetrievingJobs.stream().allMatch(Job::isSucceeded);
    if (linkedJobsAreSucceeded) {
      newZDJ.hasNewStatus(getJobStatus(newZDJ, FINISHED, SUCCEEDED));
    }
    var newStatus = newZDJ.getStatus();
    if (!oldHumanZDJStatus.getProgression().equals(newStatus.getProgression())
        || !oldHumanZDJStatus.getHealth().equals(newStatus.getHealth())) {
      jobStatusRepository.save(newStatus);
    }
    if (!oldHumanZDJ.isFinished() && newZDJ.isFinished()) {
      geoJsonConversionJobService.getOrComputeGeoJsonUrl(newZDJ.getId());
    }
    throw new RuntimeException("Fail on purpose so that message is not ack, causing retry");
  }

  private static JobStatus getJobStatus(
      ZoneDetectionJob newZDJ,
      Status.ProgressionStatus progressionStatus,
      Status.HealthStatus healthStatus) {
    return JobStatus.builder()
        .id(randomUUID().toString())
        .jobId(newZDJ.getId())
        .progression(progressionStatus)
        .health(healthStatus)
        .jobType(DETECTION)
        .creationDatetime(now())
        .build();
  }
}
