package app.bpartners.geojobs.service;

import static app.bpartners.geojobs.job.model.Status.HealthStatus.*;
import static app.bpartners.geojobs.job.model.Status.HealthStatus.UNKNOWN;
import static app.bpartners.geojobs.job.model.Status.ProgressionStatus.*;
import static app.bpartners.geojobs.repository.model.GeoJobType.DETECTION;
import static app.bpartners.geojobs.repository.model.GeoJobType.TILING;
import static java.time.Instant.now;
import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.*;

import app.bpartners.geojobs.endpoint.event.model.status.TaskStatisticRecomputingSubmitted;
import app.bpartners.geojobs.job.model.JobStatus;
import app.bpartners.geojobs.job.model.Status;
import app.bpartners.geojobs.job.model.TaskStatus;
import app.bpartners.geojobs.job.model.statistic.TaskStatistic;
import app.bpartners.geojobs.repository.*;
import app.bpartners.geojobs.repository.model.Parcel;
import app.bpartners.geojobs.repository.model.ParcelContent;
import app.bpartners.geojobs.repository.model.detection.ParcelDetectionTask;
import app.bpartners.geojobs.repository.model.detection.ZoneDetectionJob;
import app.bpartners.geojobs.repository.model.tiling.ParcelTilingTask;
import app.bpartners.geojobs.repository.model.tiling.Tile;
import app.bpartners.geojobs.repository.model.tiling.ZoneTilingJob;
import app.bpartners.geojobs.service.event.TaskStatisticRecomputingSubmittedService;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

public class TaskStatisticRecomputingSubmittedServiceTest {
  private static final String TILING_JOB_ID = "tilingJobId";
  private static final String DETECTION_JOB_ID = "detectionJobId";
  TilingTaskRepository tilingTaskRepositoryMock = mock();
  ParcelDetectionTaskRepository parcelDetectionTaskRepositoryMock = mock();
  ZoneTilingJobRepository tilingJobRepositoryMock = mock();
  ZoneDetectionJobRepository zoneDetectionJobRepositoryMock = mock();
  TaskStatisticRepository taskStatisticRepositoryMock = mock();
  TaskStatisticRecomputingSubmittedService subject =
      new TaskStatisticRecomputingSubmittedService(
          zoneDetectionJobRepositoryMock,
          parcelDetectionTaskRepositoryMock,
          tilingJobRepositoryMock,
          tilingTaskRepositoryMock,
          taskStatisticRepositoryMock);

  @BeforeEach
  void setUp() {
    when(tilingJobRepositoryMock.findById(DETECTION_JOB_ID)).thenReturn(Optional.empty());
    when(zoneDetectionJobRepositoryMock.findById(DETECTION_JOB_ID))
        .thenReturn(Optional.of(aZDJ(DETECTION_JOB_ID, PROCESSING, UNKNOWN)));
    when(taskStatisticRepositoryMock.save(any()))
        .thenAnswer(invocationOnMock -> invocationOnMock.getArgument(0));
    when(parcelDetectionTaskRepositoryMock.findAllByJobId(DETECTION_JOB_ID))
        .thenReturn(
            List.of(
                aParcelDetectionTask("detectionTask1", DETECTION_JOB_ID, PROCESSING, UNKNOWN),
                aParcelDetectionTask("detectionTask2", DETECTION_JOB_ID, PROCESSING, UNKNOWN),
                aParcelDetectionTask("detectionTask3", DETECTION_JOB_ID, PENDING, UNKNOWN),
                aParcelDetectionTask("detectionTask2", DETECTION_JOB_ID, FINISHED, SUCCEEDED),
                aParcelDetectionTask("detectionTask2", DETECTION_JOB_ID, FINISHED, FAILED)));

    when(tilingJobRepositoryMock.findById(TILING_JOB_ID))
        .thenReturn(Optional.of(aZTJ(TILING_JOB_ID, PROCESSING, UNKNOWN)));
    when(tilingTaskRepositoryMock.findAllByJobId(TILING_JOB_ID))
        .thenReturn(
            List.of(
                ParcelTilingTask.builder()
                    .jobId(TILING_JOB_ID)
                    .statusHistory(List.of())
                    .parcels(List.of())
                    .build()));
  }

  @Test
  void accept_detection_job_id_ok() {
    subject.accept(new TaskStatisticRecomputingSubmitted(DETECTION_JOB_ID));

    var taskStatisticCapture = ArgumentCaptor.forClass(TaskStatistic.class);
    verify(taskStatisticRepositoryMock, times(1)).save(taskStatisticCapture.capture());
    var actualStatistic = taskStatisticCapture.getValue();
    assertEquals(DETECTION_JOB_ID, actualStatistic.getJobId());
    assertEquals(DETECTION, actualStatistic.getJobType());
    assertFalse(actualStatistic.getTaskStatusStatistics().isEmpty());
    assertNotNull(actualStatistic);
  }

  @Test
  void accept_tiling_job_id_ok() {
    subject.accept(new TaskStatisticRecomputingSubmitted(TILING_JOB_ID));

    var taskStatisticCapture = ArgumentCaptor.forClass(TaskStatistic.class);
    verify(taskStatisticRepositoryMock, times(1)).save(taskStatisticCapture.capture());
    var actualStatistic = taskStatisticCapture.getValue();
    assertEquals(TILING_JOB_ID, actualStatistic.getJobId());
    assertEquals(TILING, actualStatistic.getJobType());
    assertFalse(actualStatistic.getTaskStatusStatistics().isEmpty());
    assertNotNull(actualStatistic);
  }

  private static ZoneDetectionJob aZDJ(
      String jobId, Status.ProgressionStatus progressionStatus, Status.HealthStatus healthStatus) {
    return ZoneDetectionJob.builder()
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
        .build();
  }

  private static ZoneTilingJob aZTJ(
      String jobId, Status.ProgressionStatus progressionStatus, Status.HealthStatus healthStatus) {
    return ZoneTilingJob.builder()
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
                    .build()))
        .build();
  }

  private static ParcelDetectionTask aParcelDetectionTask(
      String taskId,
      String jobId,
      Status.ProgressionStatus progressionStatus,
      Status.HealthStatus healthStatus) {
    return ParcelDetectionTask.builder()
        .id(taskId)
        .jobId(jobId)
        .submissionInstant(now())
        .parcels(List.of(someParcel(1)))
        .statusHistory(
            List.of(
                TaskStatus.builder()
                    .id(randomUUID().toString())
                    .progression(progressionStatus)
                    .health(healthStatus)
                    .jobType(DETECTION)
                    .taskId(taskId)
                    .creationDatetime(now())
                    .build()))
        .build();
  }

  private static Parcel someParcel(Integer parcelNb) {
    List<Tile> tiles = new ArrayList<>();
    for (int i = 0; i < parcelNb; i++) {
      tiles.add(new Tile());
    }
    return Parcel.builder().parcelContent(ParcelContent.builder().tiles(tiles).build()).build();
  }
}
