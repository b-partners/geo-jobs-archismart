package app.bpartners.geojobs.service.geojson;

import static app.bpartners.geojobs.job.model.Status.HealthStatus.UNKNOWN;
import static app.bpartners.geojobs.job.model.Status.ProgressionStatus.PENDING;
import static app.bpartners.geojobs.repository.model.GeoJobType.GEO_JSON_CONVERSION;
import static java.time.Instant.now;
import static java.util.Comparator.comparing;
import static java.util.Comparator.naturalOrder;
import static java.util.UUID.randomUUID;

import app.bpartners.geojobs.endpoint.event.EventProducer;
import app.bpartners.geojobs.endpoint.event.model.GeoJsonConversionJobCreated;
import app.bpartners.geojobs.endpoint.event.model.GeoJsonConversionJobStatusChanged;
import app.bpartners.geojobs.endpoint.rest.controller.v1.mapper.StatusMapper;
import app.bpartners.geojobs.endpoint.rest.model.GeoJsonsUrl;
import app.bpartners.geojobs.file.bucket.BucketComponent;
import app.bpartners.geojobs.job.model.JobStatus;
import app.bpartners.geojobs.job.repository.JobStatusRepository;
import app.bpartners.geojobs.job.repository.TaskRepository;
import app.bpartners.geojobs.job.service.JobService;
import app.bpartners.geojobs.repository.GeoJsonConversionJobRepository;
import app.bpartners.geojobs.repository.TaskStatisticRepository;
import app.bpartners.geojobs.repository.model.detection.Detection;
import app.bpartners.geojobs.repository.model.detection.ZoneDetectionJob;
import app.bpartners.geojobs.repository.model.geojson.GeoJsonConversionJob;
import app.bpartners.geojobs.repository.model.geojson.GeoJsonConversionTask;
import app.bpartners.geojobs.service.detection.ZoneDetectionJobService;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;

@Service
public class GeoJsonConversionJobService
    extends JobService<GeoJsonConversionTask, GeoJsonConversionJob> {
  private static final Duration PRE_SIGNED_URL_DURATION = Duration.ofHours(1L);
  private final StatusMapper<JobStatus> jobStatusMapper;
  private final ZoneDetectionJobService zoneDetectionJobService;
  private final BucketComponent bucketComponent;
  private final GeoJsonConversionJobRepository geoJsonConversionJobRepository;

  public GeoJsonConversionJobService(
      JpaRepository<GeoJsonConversionJob, String> geoJsonConversionJobRepository,
      JobStatusRepository jobStatusRepository,
      TaskStatisticRepository taskStatisticRepository,
      TaskRepository<GeoJsonConversionTask> geoJsonConversionTaskRepository,
      EventProducer eventProducer,
      StatusMapper<JobStatus> jobStatusMapper,
      ZoneDetectionJobService zoneDetectionJobService,
      BucketComponent bucketComponent,
      GeoJsonConversionJobRepository geoJsonConversionJobRepository1) {
    super(
        geoJsonConversionJobRepository,
        jobStatusRepository,
        taskStatisticRepository,
        geoJsonConversionTaskRepository,
        eventProducer,
        GeoJsonConversionJob.class);
    this.jobStatusMapper = jobStatusMapper;
    this.zoneDetectionJobService = zoneDetectionJobService;
    this.bucketComponent = bucketComponent;
    this.geoJsonConversionJobRepository = geoJsonConversionJobRepository1;
  }

  public GeoJsonsUrl getOrComputeGeoJsonUrl(String detectionJobId) {
    var zoneDetectionJob = zoneDetectionJobService.findById(detectionJobId);
    var jobStatus = zoneDetectionJob.getStatus();
    var restJobStatus = jobStatusMapper.toRest(jobStatus);
    var geoJsonConversionJob = getOrComputeGeoJsonConversionJob(zoneDetectionJob);
    return new GeoJsonsUrl().url(generatePreSignedUrl(geoJsonConversionJob)).status(restJobStatus);
  }

  private String generatePreSignedUrl(GeoJsonConversionJob geoJsonConversionJob) {
    if (geoJsonConversionJob.isSucceeded()) {
      return bucketComponent
          .presign(geoJsonConversionJob.getFileKey(), PRE_SIGNED_URL_DURATION)
          .toString();
    }
    return null;
  }

  public GeoJsonConversionJob getOrComputeGeoJsonConversionJob(
      Detection detection, ZoneDetectionJob zoneDetectionJob) {
    var geoJsonConversionJobs =
        geoJsonConversionJobRepository.findByZoneDetectionJobId(zoneDetectionJob.getId());
    if (!geoJsonConversionJobs.isEmpty()) {
      var geoJsonConversionJob =
          geoJsonConversionJobs.stream()
              .sorted(
                  comparing(GeoJsonConversionJob::getSubmissionInstant, naturalOrder()).reversed())
              .toList()
              .getFirst();
      if (detection.getGeojsonS3FileKey() != null && geoJsonConversionJob.isSucceeded()) {
        return geoJsonConversionJob;
      }
    }
    return create(zoneDetectionJob);
  }

  public GeoJsonConversionJob getOrComputeGeoJsonConversionJob(ZoneDetectionJob zoneDetectionJob) {
    var geoJsonConversionJobs =
        geoJsonConversionJobRepository.findByZoneDetectionJobId(zoneDetectionJob.getId());
    if (!geoJsonConversionJobs.isEmpty()) {
      var geoJsonConversionJob =
          geoJsonConversionJobs.stream()
              .sorted(
                  comparing(GeoJsonConversionJob::getSubmissionInstant, naturalOrder()).reversed())
              .toList()
              .getFirst();
      if (geoJsonConversionJob.isSucceeded()) {
        return geoJsonConversionJob;
      }
    }
    return create(zoneDetectionJob);
  }

  private GeoJsonConversionJob create(ZoneDetectionJob zoneDetectionJob) {
    var geoJsonConversionJob = fromZDJ(zoneDetectionJob);
    var savedGeoJsonConversion = geoJsonConversionJobRepository.save(geoJsonConversionJob);

    eventProducer.accept(List.of(new GeoJsonConversionJobCreated(savedGeoJsonConversion)));

    return savedGeoJsonConversion;
  }

  private GeoJsonConversionJob fromZDJ(ZoneDetectionJob zoneDetectionJob) {
    var jobDetectionType = zoneDetectionJob.getDetectionType();
    var geoJsonConversionJobId = randomUUID().toString();
    var geoJsonConversionJob =
        GeoJsonConversionJob.builder()
            .id(geoJsonConversionJobId)
            .emailReceiver(zoneDetectionJob.getEmailReceiver())
            .zoneName(zoneDetectionJob.getZoneName())
            .zoneDetectionJobId(zoneDetectionJob.getId())
            .zoneDetectionJobType(jobDetectionType)
            .submissionInstant(now())
            .statusHistory(new ArrayList<>())
            .build();
    geoJsonConversionJob.hasNewStatus(
        JobStatus.builder()
            .jobId(geoJsonConversionJobId)
            .id(randomUUID().toString())
            .creationDatetime(now())
            .jobType(GEO_JSON_CONVERSION)
            .progression(PENDING)
            .health(UNKNOWN)
            .build());
    return geoJsonConversionJob;
  }

  @Override
  protected void onStatusChanged(GeoJsonConversionJob oldJob, GeoJsonConversionJob newJob) {
    eventProducer.accept(
        List.of(GeoJsonConversionJobStatusChanged.builder().oldJob(oldJob).newJob(newJob).build()));
  }
}
