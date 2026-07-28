package app.bpartners.geojobs.endpoint.rest.controller;

import static app.bpartners.geojobs.job.model.Status.HealthStatus.FAILED;
import static app.bpartners.geojobs.job.model.Status.HealthStatus.RETRYING;
import static app.bpartners.geojobs.job.model.Status.HealthStatus.SUCCEEDED;
import static app.bpartners.geojobs.job.model.Status.HealthStatus.UNKNOWN;
import static app.bpartners.geojobs.job.model.Status.ProgressionStatus.FINISHED;
import static app.bpartners.geojobs.job.model.Status.ProgressionStatus.PENDING;
import static app.bpartners.geojobs.job.model.Status.ProgressionStatus.PROCESSING;
import static java.time.Instant.now;
import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import app.bpartners.geojobs.endpoint.event.EventProducer;
import app.bpartners.geojobs.endpoint.event.model.status.ZDJParcelsStatusRecomputingSubmitted;
import app.bpartners.geojobs.endpoint.event.model.status.ZDJStatusRecomputingSubmitted;
import app.bpartners.geojobs.endpoint.rest.controller.v1.ZoneDetectionController;
import app.bpartners.geojobs.endpoint.rest.controller.v1.mapper.*;
import app.bpartners.geojobs.endpoint.rest.model.*;
import app.bpartners.geojobs.file.bucket.BucketConf;
import app.bpartners.geojobs.job.model.JobStatus;
import app.bpartners.geojobs.job.model.Status;
import app.bpartners.geojobs.job.model.statistic.HealthStatusStatistic;
import app.bpartners.geojobs.job.model.statistic.TaskStatistic;
import app.bpartners.geojobs.job.model.statistic.TaskStatusStatistic;
import app.bpartners.geojobs.model.exception.BadRequestException;
import app.bpartners.geojobs.repository.DetectableObjectConfigurationRepository;
import app.bpartners.geojobs.repository.model.GeoJobType;
import app.bpartners.geojobs.repository.model.detection.DetectableObjectConfiguration;
import app.bpartners.geojobs.repository.model.detection.DetectableType;
import app.bpartners.geojobs.repository.model.detection.ZoneDetectionJob;
import app.bpartners.geojobs.repository.model.tiling.ZoneTilingJob;
import app.bpartners.geojobs.service.ParcelService;
import app.bpartners.geojobs.service.detection.ZoneDetectionJobService;
import app.bpartners.geojobs.service.geojson.GeoJsonConversionJobService;
import app.bpartners.geojobs.validator.ZoneDetectionJobValidator;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class ZoneDetectionControllerTest {
  StatusMapper<JobStatus> statusMapper = new StatusMapper<>();
  ParcelService parcelServiceMock = mock();
  DetectableObjectConfigurationRepository objectConfigurationRepositoryMock = mock();
  ZoneDetectionJobService detectionJobServiceMock = mock();
  ZoneDetectionTypeMapper zoneDetectionTypeMapper = new ZoneDetectionTypeMapper();
  ZoneDetectionJobMapper detectionJobMapper =
      new ZoneDetectionJobMapper(statusMapper, zoneDetectionTypeMapper);
  BucketConf bucketConf = mock();
  DetectableObjectConfigurationMapper objectConfigurationMapper =
      new DetectableObjectConfigurationMapper(new DetectableObjectTypeMapper(), bucketConf);
  DetectionTaskMapper taskMapper = new DetectionTaskMapper(mock());
  ZoneDetectionJobValidator jobValidator = new ZoneDetectionJobValidator(mock());
  TaskStatisticMapper taskStatisticMapper = new TaskStatisticMapper(statusMapper);
  EventProducer eventProducerMock = mock();
  GeoJsonConversionJobService geoJsonConversionJobServiceMock = mock();
  ZoneDetectionController subject =
      new ZoneDetectionController(
          parcelServiceMock,
          objectConfigurationRepositoryMock,
          detectionJobServiceMock,
          detectionJobMapper,
          objectConfigurationMapper,
          taskMapper,
          jobValidator,
          taskStatisticMapper,
          statusMapper,
          eventProducerMock,
          geoJsonConversionJobServiceMock);

  @Test
  void get_zdj_recomputed_status_ok() {
    var jobId = randomUUID().toString();
    when(detectionJobServiceMock.findById(jobId))
        .thenReturn(
            aZDJ(
                jobId,
                app.bpartners.geojobs.job.model.Status.ProgressionStatus.PENDING,
                app.bpartners.geojobs.job.model.Status.HealthStatus.UNKNOWN));

    var actual = subject.getZDJRecomputedStatus(jobId);

    var listCaptor = ArgumentCaptor.forClass(List.class);
    verify(eventProducerMock, times(1)).accept(listCaptor.capture());
    var ztjStatusComputingEvent =
        ((List<ZDJStatusRecomputingSubmitted>) listCaptor.getValue()).getFirst();
    assertEquals(new ZDJStatusRecomputingSubmitted(jobId), ztjStatusComputingEvent);
    assertEquals(
        new app.bpartners.geojobs.endpoint.rest.model.Status()
            .progression(app.bpartners.geojobs.endpoint.rest.model.Status.ProgressionEnum.PENDING)
            .health(app.bpartners.geojobs.endpoint.rest.model.Status.HealthEnum.UNKNOWN)
            .creationDatetime(actual.getCreationDatetime()),
        actual);
  }

  @Test
  void get_zdj_tasks_recomputed_status_ok() {
    var jobId = randomUUID().toString();
    when(detectionJobServiceMock.findById(jobId))
        .thenReturn(
            aZDJ(
                jobId,
                app.bpartners.geojobs.job.model.Status.ProgressionStatus.PENDING,
                app.bpartners.geojobs.job.model.Status.HealthStatus.UNKNOWN));

    var actual = subject.getZDJTasksRecomputedStatus(jobId);

    var listCaptor = ArgumentCaptor.forClass(List.class);
    verify(eventProducerMock, times(1)).accept(listCaptor.capture());
    var ztjStatusComputingEvent =
        ((List<ZDJParcelsStatusRecomputingSubmitted>) listCaptor.getValue()).getFirst();
    assertEquals(new ZDJParcelsStatusRecomputingSubmitted(jobId), ztjStatusComputingEvent);
    assertEquals(
        new app.bpartners.geojobs.endpoint.rest.model.Status()
            .progression(app.bpartners.geojobs.endpoint.rest.model.Status.ProgressionEnum.PENDING)
            .health(app.bpartners.geojobs.endpoint.rest.model.Status.HealthEnum.UNKNOWN)
            .creationDatetime(actual.getCreationDatetime()),
        actual);
  }

  @Test
  void get_detection_task_statistics_ok() {
    var jobId = randomUUID().toString();
    var domainStatistic = aTaskStatistic(jobId);
    var expected = taskStatisticMapper.toRest(domainStatistic);
    when(detectionJobServiceMock.computeTaskStatistics(jobId)).thenReturn(domainStatistic);

    var actual = subject.getDetectionTaskStatistics(jobId);

    assertEquals(expected, actual);
  }

  @Test
  void succeedJob_whenJobNotSucceeded_throwsBadRequestException() {
    var jobId = randomUUID().toString();
    var job = mock(ZoneDetectionJob.class);
    when(job.isSucceeded()).thenReturn(false);
    when(detectionJobServiceMock.findById(jobId)).thenReturn(job);

    BadRequestException actual =
        assertThrows(BadRequestException.class, () -> subject.succeedJob(jobId));

    assertTrue(actual.getMessage().contains("Zone detection on status"));
    verify(detectionJobServiceMock).findById(jobId);
    verify(objectConfigurationRepositoryMock, never()).findAllByDetectionJobId(anyString());
    verify(eventProducerMock, never()).accept(anyList());
  }

  @Test
  void succeedJob_ok() {
    var jobId = randomUUID().toString();
    var job = mock(ZoneDetectionJob.class);
    var tilingJob = mock(ZoneTilingJob.class);
    var status = mock(JobStatus.class);
    var mockConfig =
        mock(app.bpartners.geojobs.repository.model.detection.DetectableObjectConfiguration.class);
    List<DetectableObjectConfiguration> objectConfigs = List.of(mockConfig);

    when(job.isSucceeded()).thenReturn(true);
    when(job.getId()).thenReturn(jobId);
    when(job.getZoneTilingJob()).thenReturn(tilingJob);
    when(tilingJob.getId()).thenReturn("tilingJobId");
    when(mockConfig.getObjectType()).thenReturn(DetectableType.TROTTOIR);
    when(job.getStatus()).thenReturn(status);
    when(job.getStatusHistory()).thenReturn(List.of(status));
    when(status.getProgression()).thenReturn(FINISHED);
    when(status.getHealth()).thenReturn(SUCCEEDED);
    when(detectionJobServiceMock.findById(jobId)).thenReturn(job);
    when(objectConfigurationRepositoryMock.findAllByDetectionJobId(jobId))
        .thenReturn(objectConfigs);

    var actual = subject.succeedJob(jobId);

    assertNotNull(actual);
    assertEquals(jobId, actual.getId());
    assertEquals("tilingJobId", actual.getZoneTilingJobId());
    assertNotNull(actual.getStatus());
    assertEquals(FINISHED.name(), actual.getStatus().getProgression().name());
    assertEquals(SUCCEEDED.name(), actual.getStatus().getHealth().name());
    assertNotNull(actual.getObjectsToDetect());
    assertFalse(actual.getObjectsToDetect().isEmpty());
  }

  private static app.bpartners.geojobs.repository.model.detection.ZoneDetectionJob aZDJ(
      String jobId, Status.ProgressionStatus progressionStatus, Status.HealthStatus healthStatus) {
    return app.bpartners.geojobs.repository.model.detection.ZoneDetectionJob.builder()
        .id(jobId)
        .zoneName("dummy")
        .emailReceiver("dummy")
        .statusHistory(
            List.of(
                JobStatus.builder()
                    .id(randomUUID().toString())
                    .jobId(jobId)
                    .progression(progressionStatus)
                    .health(healthStatus)
                    .creationDatetime(now())
                    .build()))
        .zoneTilingJob(new ZoneTilingJob())
        .build();
  }

  private static TaskStatistic aTaskStatistic(String jobId) {
    var statusStatistics =
        List.of(
            aStatusStatistic(PENDING), aStatusStatistic(PROCESSING), aStatusStatistic(FINISHED));
    var taskStatistic =
        TaskStatistic.builder()
            .id("taskStatisticId")
            .jobId(jobId)
            .actualJobStatus(
                JobStatus.builder()
                    .progression(PENDING)
                    .health(UNKNOWN)
                    .creationDatetime(now())
                    .build())
            .updatedAt(now())
            .jobType(GeoJobType.DETECTION)
            .build();
    taskStatistic.addStatusStatistics(statusStatistics);
    return taskStatistic;
  }

  private static TaskStatusStatistic aStatusStatistic(Status.ProgressionStatus progressionStatus) {
    var healthStatusStatistics =
        List.of(
            aHealthStatusStatistic(UNKNOWN),
            aHealthStatusStatistic(RETRYING),
            aHealthStatusStatistic(FAILED),
            aHealthStatusStatistic(SUCCEEDED));
    var taskStatusStatistic = TaskStatusStatistic.builder().progression(progressionStatus).build();
    taskStatusStatistic.addHealthStatusStatistics(healthStatusStatistics);
    return taskStatusStatistic;
  }

  private static HealthStatusStatistic aHealthStatusStatistic(Status.HealthStatus healthStatus) {
    return HealthStatusStatistic.builder().healthStatus(healthStatus).count(1L).build();
  }
}
