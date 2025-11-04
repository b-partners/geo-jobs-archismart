package app.bpartners.geojobs.service;

import static app.bpartners.geojobs.job.model.Status.HealthStatus.*;
import static app.bpartners.geojobs.job.model.Status.ProgressionStatus.*;
import static app.bpartners.geojobs.repository.model.GeoJobType.DETECTION;
import static app.bpartners.geojobs.repository.model.detection.DetectableType.TOITURE_REVETEMENT;
import static app.bpartners.geojobs.repository.model.detection.ZoneDetectionJob.DetectionType.HUMAN;
import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import app.bpartners.geojobs.concurrency.Workers;
import app.bpartners.geojobs.endpoint.event.EventProducer;
import app.bpartners.geojobs.endpoint.event.model.AutoTaskStatisticRecomputingSubmitted;
import app.bpartners.geojobs.job.model.JobStatus;
import app.bpartners.geojobs.job.model.Status;
import app.bpartners.geojobs.job.model.TaskStatus;
import app.bpartners.geojobs.job.model.statistic.TaskStatistic;
import app.bpartners.geojobs.job.repository.JobStatusRepository;
import app.bpartners.geojobs.model.exception.NotImplementedException;
import app.bpartners.geojobs.repository.*;
import app.bpartners.geojobs.repository.model.Parcel;
import app.bpartners.geojobs.repository.model.annotation.AnnotationDeliveryConfiguration;
import app.bpartners.geojobs.repository.model.detection.DetectableObjectConfiguration;
import app.bpartners.geojobs.repository.model.detection.ParcelDetectionTask;
import app.bpartners.geojobs.repository.model.detection.ZoneDetectionJob;
import app.bpartners.geojobs.repository.model.tiling.ZoneTilingJob;
import app.bpartners.geojobs.service.detection.ZoneDetectionJobService;
import jakarta.persistence.EntityManager;
import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.jpa.repository.JpaRepository;

@Slf4j
class ZoneDetectionJobServiceTest {
  private static final String JOB_ID = "jobId";
  private static final String JOB_3_ID = "job3_id";
  private static final String JOB_4_ID = "job4_id";
  JpaRepository<ZoneDetectionJob, String> jobRepositoryMock = mock();
  JobStatusRepository jobStatusRepositoryMock = mock();
  ParcelDetectionTaskRepository taskRepositoryMock = mock();
  EventProducer eventProducerMock = mock();
  EntityManager entityManagerMock = mock();
  ZoneDetectionJobRepository zoneDetectionJobRepositoryMock = mock();
  TaskStatisticRepository taskStatisticRepositoryMock = mock();
  AnnotationDeliveryConfigurationRepository deliveryConfigurationRepositoryMock = mock();
  MachineDetectedTileRepository machineDetectedTileRepositoryMock = mock();
  TileDetectionTaskConsumer tileDetectionTaskConsumerMock = mock();
  Workers workersMock = mock();
  ZoneDetectionJobService subject =
      new ZoneDetectionJobService(
          jobRepositoryMock,
          jobStatusRepositoryMock,
          mock(),
          taskRepositoryMock,
          eventProducerMock,
          mock(),
          mock(),
          mock(),
          zoneDetectionJobRepositoryMock,
          taskStatisticRepositoryMock,
          machineDetectedTileRepositoryMock,
          deliveryConfigurationRepositoryMock,
          tileDetectionTaskConsumerMock,
          workersMock);

  @BeforeEach
  void setUp() {
    doNothing().when(entityManagerMock).detach(any());
    subject.setEm(entityManagerMock);
  }

  @Test
  void auto_task_statistic_event_sent_ok() {
    String jobId = "jobId";
    when(jobRepositoryMock.findById(jobId))
        .thenReturn(
            Optional.of(
                ZoneDetectionJob.builder()
                    .id(jobId)
                    .detectionType(HUMAN)
                    .zoneTilingJob(new ZoneTilingJob())
                    .build()));
    var objectConfiguration =
        DetectableObjectConfiguration.builder()
            .detectionJobId(jobId)
            .bucketStorageName("bucketStorageName")
            .objectType(TOITURE_REVETEMENT)
            .minConfidenceForDetection(1.0)
            .build();

    assertDoesNotThrow(() -> subject.fireTasks(jobId, List.of(objectConfiguration)));

    var listCaptor = ArgumentCaptor.forClass(List.class);
    verify(eventProducerMock, times(2)).accept(listCaptor.capture());
    List<List> allValues = listCaptor.getAllValues();
    var taskStatisticComputingEvent =
        ((List<AutoTaskStatisticRecomputingSubmitted>) allValues.getLast()).getFirst();
    assertEquals(new AutoTaskStatisticRecomputingSubmitted(jobId), taskStatisticComputingEvent);
  }

  @Test
  void process_zdj_ko() {
    when(jobRepositoryMock.findById(JOB_4_ID))
        .thenReturn(
            Optional.of(
                ZoneDetectionJob.builder()
                    .statusHistory(
                        List.of(
                            JobStatus.builder().progression(PROCESSING).health(UNKNOWN).build()))
                    .zoneTilingJob(ZoneTilingJob.builder().id("zoneTilingJobId").build())
                    .build()));
    when(zoneDetectionJobRepositoryMock.findAllByZoneTilingJob_Id("zoneTilingJobId"))
        .thenReturn(List.of(ZoneDetectionJob.builder().detectionType(HUMAN).build()));

    assertThrows(
        NotImplementedException.class,
        () ->
            subject.fireTasks(
                JOB_4_ID,
                List.of(
                    DetectableObjectConfiguration.builder()
                        .bucketStorageName("bucketStorageName")
                        .build(),
                    DetectableObjectConfiguration.builder()
                        .bucketStorageName("otherBucketStorageName")
                        .build())));
  }

  @Test
  void read_task_statistics_ok() {
    JobStatus failedProcessingStatus =
        JobStatus.builder().progression(PROCESSING).health(FAILED).jobType(DETECTION).build();
    when(jobRepositoryMock.findById(JOB_3_ID))
        .thenReturn(
            Optional.of(
                ZoneDetectionJob.builder()
                    .id(JOB_3_ID)
                    .statusHistory(List.of(failedProcessingStatus))
                    .build()));
    JobStatus pendingJobStatus =
        JobStatus.builder().progression(PENDING).health(UNKNOWN).jobType(DETECTION).build();
    when(jobRepositoryMock.findById(JOB_ID))
        .thenReturn(
            Optional.of(
                ZoneDetectionJob.builder()
                    .id(JOB_ID)
                    .statusHistory(List.of(pendingJobStatus))
                    .build()));
    TaskStatistic expected =
        TaskStatistic.builder().actualJobStatus(failedProcessingStatus).build();
    when(taskStatisticRepositoryMock.findTopByJobIdOrderByUpdatedAtDesc(JOB_3_ID))
        .thenReturn(expected);
    when(taskStatisticRepositoryMock.findTopByJobIdOrderByUpdatedAtDesc(JOB_ID)).thenReturn(null);

    TaskStatistic actual = subject.computeTaskStatistics(JOB_3_ID);
    TaskStatistic actual2 = subject.computeTaskStatistics(JOB_ID);

    assertEquals(expected, actual);
    assertEquals(
        TaskStatistic.builder()
            .id(actual2.getId())
            .jobId(JOB_ID)
            .taskStatusStatistics(List.of())
            .actualJobStatus(pendingJobStatus)
            .jobType(pendingJobStatus.getJobType())
            .updatedAt(actual2.getUpdatedAt())
            .build(),
        actual2);
  }

  static ParcelDetectionTask taskWithStatus(
      Status.ProgressionStatus progressionStatus, Status.HealthStatus healthStatus) {
    return taskWithStatus(progressionStatus, healthStatus, null);
  }

  static ParcelDetectionTask taskWithStatus(
      Status.ProgressionStatus progressionStatus, Status.HealthStatus healthStatus, Parcel parcel) {
    return ParcelDetectionTask.builder()
        .statusHistory(
            List.of(
                TaskStatus.builder()
                    .id(randomUUID().toString())
                    .progression(progressionStatus)
                    .jobType(DETECTION)
                    .health(healthStatus)
                    .build()))
        .parcels(parcel == null ? null : List.of(parcel))
        .build();
  }

  @Test
  void count_in_doubt_machine_detected_tiles() {
    var jobId = randomUUID().toString();
    var minimumConfidenceForDelivery = 1.0;
    when(deliveryConfigurationRepositoryMock.findLatestConfiguration())
        .thenReturn(
            Optional.of(
                AnnotationDeliveryConfiguration.builder()
                    .minimumConfidenceForDelivery(minimumConfidenceForDelivery)
                    .build()));
    var expected = 0L;
    when(machineDetectedTileRepositoryMock.countInDoubtDetectedTileToDeliveryByZdjJobId(
            jobId, minimumConfidenceForDelivery))
        .thenReturn(expected);

    var actual = subject.countInDoubtDetectedTileToDeliveryById(jobId);

    assertEquals(expected, actual);
  }
}
