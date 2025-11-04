package app.bpartners.geojobs.service;

import static app.bpartners.geojobs.job.model.Status.HealthStatus.SUCCEEDED;
import static app.bpartners.geojobs.job.model.Status.HealthStatus.UNKNOWN;
import static app.bpartners.geojobs.job.model.Status.ProgressionStatus.FINISHED;
import static app.bpartners.geojobs.job.model.Status.ProgressionStatus.PENDING;
import static java.time.Instant.now;
import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import app.bpartners.geojobs.conf.FacadeIT;
import app.bpartners.geojobs.endpoint.event.model.zone.ImportedZoneTilingJobSaved;
import app.bpartners.geojobs.endpoint.rest.model.BucketSeparatorType;
import app.bpartners.geojobs.endpoint.rest.model.GeoServerParameter;
import app.bpartners.geojobs.job.model.JobStatus;
import app.bpartners.geojobs.repository.TilingTaskRepository;
import app.bpartners.geojobs.repository.ZoneTilingJobRepository;
import app.bpartners.geojobs.repository.model.tiling.ParcelTilingTask;
import app.bpartners.geojobs.repository.model.tiling.ZoneTilingJob;
import app.bpartners.geojobs.service.event.ImportedZoneTilingJobSavedService;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

public class ImportedZoneTilingJobSavedServiceIT extends FacadeIT {
  private static final String JOB_ID = "importedJobId";
  @Autowired ImportedZoneTilingJobSavedService subject;
  @Autowired ZoneTilingJobRepository tilingJobRepository;
  @Autowired TilingTaskRepository tilingTaskRepository;

  @BeforeEach
  void setUp() {
    tilingJobRepository.save(zoneTilingJob());
  }

  private static ZoneTilingJob zoneTilingJob() {
    return ZoneTilingJob.builder()
        .id(JOB_ID)
        .zoneName("dummyZoneName")
        .emailReceiver("dummyEmailReceiver")
        .statusHistory(
            List.of(
                JobStatus.builder()
                    .id(randomUUID().toString())
                    .jobId(JOB_ID)
                    .progression(PENDING)
                    .health(UNKNOWN)
                    .creationDatetime(now())
                    .build()))
        .build();
  }

  @AfterEach
  void tearDown() {
    tilingTaskRepository.deleteAll();
    tilingJobRepository.deleteById(JOB_ID);
  }

  @Disabled("TODO: run in local only, add AWS credentials and edit EventConf.aws.region")
  @Test
  void accept_ok() {
    Long startFrom = 0L;
    Long endAt = 1L;
    String dummyBucketName = "cannes-draft";
    String dummyBucketPathPrefix = "draft_layer";
    GeoServerParameter geoServerParameter = new GeoServerParameter();
    String dummyGeoServerUrl = "https://dummyGeoServerUrl.com";
    List<ParcelTilingTask> tasksBefore = tilingTaskRepository.findAllByJobId(JOB_ID);
    ZoneTilingJob jobBefore = tilingJobRepository.findById(JOB_ID).orElseThrow();

    subject.accept(
        new ImportedZoneTilingJobSaved(
            startFrom,
            endAt,
            JOB_ID,
            dummyBucketName,
            dummyBucketPathPrefix,
            geoServerParameter,
            dummyGeoServerUrl,
            BucketSeparatorType.SLASH));

    List<ParcelTilingTask> tasksActual = tilingTaskRepository.findAllByJobId(JOB_ID);
    ZoneTilingJob jobActual = tilingJobRepository.findById(JOB_ID).orElseThrow();
    assertTrue(tasksBefore.isEmpty());
    assertEquals(
        truncatedDateTimes(
            zoneTilingJob().toBuilder()
                .submissionInstant(jobBefore.getSubmissionInstant())
                .build()),
        truncatedDateTimes(jobBefore));
    assertEquals(3, tasksActual.size());
    assertEquals(
        jobBefore.toBuilder().statusHistory(List.of()).build(),
        jobActual.toBuilder().statusHistory(List.of()).build());
    assertEquals(
        JobStatus.builder()
            .progression(FINISHED)
            .health(SUCCEEDED)
            .creationDatetime(jobActual.getStatus().getCreationDatetime())
            .build(),
        jobActual.getStatus());
  }

  private ZoneTilingJob truncatedDateTimes(ZoneTilingJob job) {
    job.getStatusHistory()
        .forEach(
            jobStatus ->
                jobStatus.setCreationDatetime(
                    jobStatus.getCreationDatetime().truncatedTo(ChronoUnit.MINUTES)));
    return job;
  }
}
