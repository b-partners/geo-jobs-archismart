package app.bpartners.geojobs.service;

import static app.bpartners.geojobs.job.model.Status.HealthStatus.SUCCEEDED;
import static app.bpartners.geojobs.job.model.Status.HealthStatus.UNKNOWN;
import static app.bpartners.geojobs.job.model.Status.ProgressionStatus.FINISHED;
import static app.bpartners.geojobs.job.model.Status.ProgressionStatus.PENDING;
import static app.bpartners.geojobs.repository.model.GeoJobType.GEO_JSON_CONVERSION;
import static app.bpartners.geojobs.repository.model.detection.ZoneDetectionJob.DetectionType.HUMAN;
import static java.time.Instant.now;
import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import app.bpartners.geojobs.endpoint.event.EventProducer;
import app.bpartners.geojobs.endpoint.event.model.GeoJsonConversionJobCreated;
import app.bpartners.geojobs.endpoint.rest.controller.mapper.StatusMapper;
import app.bpartners.geojobs.file.bucket.BucketComponent;
import app.bpartners.geojobs.job.model.JobStatus;
import app.bpartners.geojobs.job.model.Status;
import app.bpartners.geojobs.job.repository.JobStatusRepository;
import app.bpartners.geojobs.job.repository.TaskRepository;
import app.bpartners.geojobs.repository.GeoJsonConversionJobRepository;
import app.bpartners.geojobs.repository.TaskStatisticRepository;
import app.bpartners.geojobs.repository.model.detection.Detection;
import app.bpartners.geojobs.repository.model.detection.ZoneDetectionJob;
import app.bpartners.geojobs.repository.model.geojson.GeoJsonConversionJob;
import app.bpartners.geojobs.repository.model.geojson.GeoJsonConversionTask;
import app.bpartners.geojobs.service.detection.ZoneDetectionJobService;
import app.bpartners.geojobs.service.geojson.GeoJsonConversionJobService;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.jpa.repository.JpaRepository;

class GeoJsonConversionJobServiceTest {
  JpaRepository<GeoJsonConversionJob, String> geoJsonConversionJobRepositoryMock = mock();
  JobStatusRepository jobStatusRepositoryMock = mock();
  TaskStatisticRepository taskStatisticRepositoryMock = mock();
  TaskRepository<GeoJsonConversionTask> geoJsonConversionTaskRepositoryMock = mock();
  EventProducer eventProducerMock = mock();
  StatusMapper<JobStatus> jobStatusMapper = new StatusMapper<>();
  ZoneDetectionJobService zoneDetectionJobServiceMock = mock();
  BucketComponent bucketComponentMock = mock();
  GeoJsonConversionJobRepository geoJsonConversionJobRepository = mock();
  GeoJsonConversionJobService subject =
      new GeoJsonConversionJobService(
          geoJsonConversionJobRepositoryMock,
          jobStatusRepositoryMock,
          taskStatisticRepositoryMock,
          geoJsonConversionTaskRepositoryMock,
          eventProducerMock,
          jobStatusMapper,
          zoneDetectionJobServiceMock,
          bucketComponentMock,
          geoJsonConversionJobRepository);

  @Test
  void compute_geo_json_conversion_job_with_detection_and_zdj() {
    var detection = mock(Detection.class);
    var zoneDetectionJob = mock(ZoneDetectionJob.class);
    var zoneDetectionJobId = randomUUID().toString();
    when(zoneDetectionJob.getId()).thenReturn(zoneDetectionJobId);
    var detectionJobType = HUMAN;
    when(zoneDetectionJob.getDetectionType()).thenReturn(detectionJobType);
    when(geoJsonConversionJobRepository.findByZoneDetectionJobId(zoneDetectionJob.getId()))
        .thenReturn(List.of());
    when(geoJsonConversionJobRepository.save(any()))
        .thenAnswer(invocation -> invocation.getArgument(0));

    var actual = subject.getOrComputeGeoJsonConversionJob(detection, zoneDetectionJob);

    var expectedGeoJsonConversionJob =
        computeExpectedGeoJsonFromActual(actual, zoneDetectionJobId, detectionJobType);
    var expectedGeoJsonConversionJobCreated = new GeoJsonConversionJobCreated(actual);
    var eventCaptor = ArgumentCaptor.forClass(List.class);
    verify(eventProducerMock, only()).accept(eventCaptor.capture());
    var actualGeoJsonConversionJobCreated =
        (GeoJsonConversionJobCreated) eventCaptor.getValue().getFirst();
    assertEquals(expectedGeoJsonConversionJob, actual);
    assertEquals(expectedGeoJsonConversionJobCreated, actualGeoJsonConversionJobCreated);
  }

  @Test
  void compute_geo_json_conversion_job_with_zdj() {
    var zoneDetectionJob = mock(ZoneDetectionJob.class);
    var zoneDetectionJobId = randomUUID().toString();
    when(zoneDetectionJob.getId()).thenReturn(zoneDetectionJobId);
    var detectionJobType = HUMAN;
    when(zoneDetectionJob.getDetectionType()).thenReturn(detectionJobType);
    when(geoJsonConversionJobRepository.findByZoneDetectionJobId(zoneDetectionJob.getId()))
        .thenReturn(List.of());
    when(geoJsonConversionJobRepository.save(any()))
        .thenAnswer(invocation -> invocation.getArgument(0));

    var actual = subject.getOrComputeGeoJsonConversionJob(zoneDetectionJob);

    var expectedGeoJsonConversionJob =
        computeExpectedGeoJsonFromActual(actual, zoneDetectionJobId, detectionJobType);
    var expectedGeoJsonConversionJobCreated = new GeoJsonConversionJobCreated(actual);
    var eventCaptor = ArgumentCaptor.forClass(List.class);
    verify(eventProducerMock, only()).accept(eventCaptor.capture());
    var actualGeoJsonConversionJobCreated =
        (GeoJsonConversionJobCreated) eventCaptor.getValue().getFirst();
    assertEquals(expectedGeoJsonConversionJob, actual);
    assertEquals(expectedGeoJsonConversionJobCreated, actualGeoJsonConversionJobCreated);
  }

  @Test
  void get_geo_json_conversion_job_when_job_succeeded_and_detection_geo_json_url_not_null() {
    var detection = mock(Detection.class);
    var zoneDetectionJob = mock(ZoneDetectionJob.class);
    var zoneDetectionJobId = randomUUID().toString();
    var detectionJobType = HUMAN;
    var geoJsonConversionJobId = randomUUID().toString();
    var jobStatusId = randomUUID().toString();
    var geoJsonConversionJob =
        createGeoJsonConversionJob(
            geoJsonConversionJobId,
            jobStatusId,
            zoneDetectionJobId,
            detectionJobType,
            FINISHED,
            SUCCEEDED);

    when(detection.getGeojsonS3FileKey()).thenReturn("notNullFileKey");
    when(zoneDetectionJob.getId()).thenReturn(zoneDetectionJobId);
    when(zoneDetectionJob.getDetectionType()).thenReturn(detectionJobType);
    when(geoJsonConversionJobRepository.findByZoneDetectionJobId(zoneDetectionJob.getId()))
        .thenReturn(List.of(geoJsonConversionJob));

    var actual = subject.getOrComputeGeoJsonConversionJob(detection, zoneDetectionJob);

    verify(eventProducerMock, never()).accept(any());
    verify(geoJsonConversionJobRepositoryMock, never()).save(any());
    assertEquals(geoJsonConversionJob, actual);
  }

  @Test
  void get_geo_json_conversion_job_when_job_succeeded() {
    var zoneDetectionJob = mock(ZoneDetectionJob.class);
    var zoneDetectionJobId = randomUUID().toString();
    var detectionJobType = HUMAN;
    var geoJsonConversionJobId = randomUUID().toString();
    var jobStatusId = randomUUID().toString();
    var geoJsonConversionJob =
        createGeoJsonConversionJob(
            geoJsonConversionJobId,
            jobStatusId,
            zoneDetectionJobId,
            detectionJobType,
            FINISHED,
            SUCCEEDED);

    when(zoneDetectionJob.getId()).thenReturn(zoneDetectionJobId);
    when(zoneDetectionJob.getDetectionType()).thenReturn(detectionJobType);
    when(geoJsonConversionJobRepository.findByZoneDetectionJobId(zoneDetectionJob.getId()))
        .thenReturn(List.of(geoJsonConversionJob));

    var actual = subject.getOrComputeGeoJsonConversionJob(zoneDetectionJob);

    verify(eventProducerMock, never()).accept(any());
    verify(geoJsonConversionJobRepositoryMock, never()).save(any());
    assertEquals(geoJsonConversionJob, actual);
  }

  private GeoJsonConversionJob computeExpectedGeoJsonFromActual(
      GeoJsonConversionJob actual,
      String zoneDetectionJobId,
      ZoneDetectionJob.DetectionType detectionJobType) {
    return createGeoJsonConversionJob(
        actual.getId(),
        actual.getStatus().getId(),
        zoneDetectionJobId,
        detectionJobType,
        PENDING,
        UNKNOWN);
  }

  private GeoJsonConversionJob createGeoJsonConversionJob(
      String geoJsonConversionJobId,
      String jobStatusId,
      String zoneDetectionJobId,
      ZoneDetectionJob.DetectionType detectionJobType,
      Status.ProgressionStatus progressionStatus,
      Status.HealthStatus healthStatus) {
    var statusHistory = new ArrayList<JobStatus>();
    statusHistory.add(
        JobStatus.builder()
            .id(jobStatusId)
            .jobId(geoJsonConversionJobId)
            .creationDatetime(now())
            .jobType(GEO_JSON_CONVERSION)
            .progression(progressionStatus)
            .health(healthStatus)
            .build());
    return GeoJsonConversionJob.builder()
        .id(geoJsonConversionJobId)
        .zoneDetectionJobId(zoneDetectionJobId)
        .zoneDetectionJobType(detectionJobType)
        .submissionInstant(now())
        .statusHistory(statusHistory)
        .build();
  }
}
