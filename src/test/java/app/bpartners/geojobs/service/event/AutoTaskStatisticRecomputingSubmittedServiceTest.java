package app.bpartners.geojobs.service.event;

import static app.bpartners.geojobs.job.model.Status.HealthStatus.FAILED;
import static app.bpartners.geojobs.job.model.Status.HealthStatus.SUCCEEDED;
import static app.bpartners.geojobs.job.model.Status.HealthStatus.UNKNOWN;
import static app.bpartners.geojobs.job.model.Status.ProgressionStatus.FINISHED;
import static app.bpartners.geojobs.job.model.Status.ProgressionStatus.PENDING;
import static app.bpartners.geojobs.job.model.Status.ProgressionStatus.PROCESSING;
import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import app.bpartners.geojobs.endpoint.event.model.AutoTaskStatisticRecomputingSubmitted;
import app.bpartners.geojobs.job.model.JobStatus;
import app.bpartners.geojobs.job.model.Status;
import app.bpartners.geojobs.model.exception.ApiException;
import app.bpartners.geojobs.model.exception.NotFoundException;
import app.bpartners.geojobs.repository.ParcelDetectionTaskRepository;
import app.bpartners.geojobs.repository.TaskStatisticRepository;
import app.bpartners.geojobs.repository.TilingTaskRepository;
import app.bpartners.geojobs.repository.ZoneDetectionJobRepository;
import app.bpartners.geojobs.repository.ZoneTilingJobRepository;
import app.bpartners.geojobs.repository.model.detection.ZoneDetectionJob;
import app.bpartners.geojobs.repository.model.tiling.ZoneTilingJob;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

public class AutoTaskStatisticRecomputingSubmittedServiceTest {
  private static final String TILING_JOB_ID = "tilingJobId";
  private static final String DETECTION_JOB_ID = "detection_job_id";
  private static final String NOT_FOUND_JOB = "notFoundJob";
  private static final String NOT_FINISHED_JOB = "notFinishedJob";
  ZoneDetectionJobRepository zoneDetectionRepositoryMock = mock();
  ParcelDetectionTaskRepository parcelDetectionTaskRepositoryMock = mock();
  ZoneTilingJobRepository tilingJobRepositoryMock = mock();
  TilingTaskRepository tilingTaskRepositoryMock = mock();
  TaskStatisticRepository taskStatisticRepositoryMock = mock();
  AutoTaskStatisticRecomputingSubmittedService subject =
      new AutoTaskStatisticRecomputingSubmittedService(
          zoneDetectionRepositoryMock,
          parcelDetectionTaskRepositoryMock,
          tilingJobRepositoryMock,
          tilingTaskRepositoryMock,
          taskStatisticRepositoryMock);

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
                    .build()))
        .build();
  }

  @Test
  void accept_tiling_ok() {
    when(tilingJobRepositoryMock.findById(TILING_JOB_ID))
        .thenReturn(Optional.of(aZTJ(TILING_JOB_ID, PROCESSING, UNKNOWN)));
    var statComputeEvent = new AutoTaskStatisticRecomputingSubmitted(TILING_JOB_ID);

    assertThrows(ApiException.class, () -> subject.accept(statComputeEvent));
    verify(tilingJobRepositoryMock, times(2)).findById(TILING_JOB_ID);
    verify(taskStatisticRepositoryMock, times(1)).save(any());
  }

  @Test
  void accept_detection_ok() {
    when(zoneDetectionRepositoryMock.findById(DETECTION_JOB_ID))
        .thenReturn(Optional.of(aZDJ(DETECTION_JOB_ID, PROCESSING, UNKNOWN)));
    var statComputeEvent = new AutoTaskStatisticRecomputingSubmitted(DETECTION_JOB_ID);

    assertThrows(ApiException.class, () -> subject.accept(statComputeEvent));
    verify(zoneDetectionRepositoryMock, times(2)).findById(DETECTION_JOB_ID);
    verify(taskStatisticRepositoryMock, times(1)).save(any());
  }

  @Test
  void accept_detection_ko() {
    when(tilingJobRepositoryMock.findById(NOT_FOUND_JOB)).thenReturn(Optional.empty());
    when(zoneDetectionRepositoryMock.findById(NOT_FOUND_JOB)).thenReturn(Optional.empty());

    assertThrows(
        NotFoundException.class,
        () -> subject.accept(new AutoTaskStatisticRecomputingSubmitted(NOT_FOUND_JOB)));
  }

  @Test
  void accept_finished_job_or_max_attempt_reached_ok() {
    when(tilingJobRepositoryMock.findById(NOT_FINISHED_JOB))
        .thenReturn(Optional.of(aZTJ(NOT_FINISHED_JOB, PENDING, UNKNOWN)));
    when(tilingJobRepositoryMock.findById(TILING_JOB_ID))
        .thenReturn(Optional.of(aZTJ(TILING_JOB_ID, FINISHED, SUCCEEDED)));
    when(zoneDetectionRepositoryMock.findById(DETECTION_JOB_ID))
        .thenReturn(Optional.of(aZDJ(DETECTION_JOB_ID, FINISHED, FAILED)));
    var notFinishedJob = new AutoTaskStatisticRecomputingSubmitted(NOT_FINISHED_JOB);
    notFinishedJob.setAttemptNb(7);

    assertDoesNotThrow(() -> subject.accept(notFinishedJob));
    assertDoesNotThrow(
        () -> subject.accept(new AutoTaskStatisticRecomputingSubmitted(TILING_JOB_ID)));
    assertDoesNotThrow(
        () -> subject.accept(new AutoTaskStatisticRecomputingSubmitted(DETECTION_JOB_ID)));
    verify(taskStatisticRepositoryMock, times(0)).save(any());
  }
}
