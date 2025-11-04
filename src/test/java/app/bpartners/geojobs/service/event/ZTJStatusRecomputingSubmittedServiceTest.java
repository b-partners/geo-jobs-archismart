package app.bpartners.geojobs.service.event;

import static app.bpartners.geojobs.job.model.Status.HealthStatus.FAILED;
import static app.bpartners.geojobs.job.model.Status.HealthStatus.SUCCEEDED;
import static app.bpartners.geojobs.job.model.Status.HealthStatus.UNKNOWN;
import static app.bpartners.geojobs.job.model.Status.ProgressionStatus.FINISHED;
import static app.bpartners.geojobs.job.model.Status.ProgressionStatus.PROCESSING;
import static java.time.Instant.now;
import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import app.bpartners.geojobs.endpoint.event.model.status.ZTJStatusRecomputingSubmitted;
import app.bpartners.geojobs.job.model.JobStatus;
import app.bpartners.geojobs.job.model.Status.HealthStatus;
import app.bpartners.geojobs.job.model.Status.ProgressionStatus;
import app.bpartners.geojobs.job.model.TaskStatus;
import app.bpartners.geojobs.job.repository.TaskRepository;
import app.bpartners.geojobs.job.service.TaskStatusService;
import app.bpartners.geojobs.repository.ZoneTilingJobRepository;
import app.bpartners.geojobs.repository.model.tiling.ParcelTilingTask;
import app.bpartners.geojobs.repository.model.tiling.ZoneTilingJob;
import app.bpartners.geojobs.service.tiling.ZoneTilingJobService;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class ZTJStatusRecomputingSubmittedServiceTest {
  ZoneTilingJobService tilingJobServiceMock = mock();
  TaskStatusService<ParcelTilingTask> taskStatusServiceMock = mock();
  TaskRepository<ParcelTilingTask> taskRepositoryMock = mock();
  ZoneTilingJobRepository zoneTilingJobRepository = mock();
  ZTJStatusRecomputingSubmittedService subject =
      new ZTJStatusRecomputingSubmittedService(
          tilingJobServiceMock, taskStatusServiceMock, taskRepositoryMock);

  @Test
  void accept_max_attempt_reached() {
    String processingJob = "jobId";
    ZoneTilingJob zoneTilingJob = aZTJ(processingJob, PROCESSING, UNKNOWN);
    when(tilingJobServiceMock.findById(processingJob)).thenReturn(zoneTilingJob);
    when(tilingJobServiceMock.recomputeStatus(zoneTilingJob)).thenReturn(zoneTilingJob);
    when(taskRepositoryMock.findAllByJobId(processingJob))
        .thenReturn(
            List.of(
                aTilingTask(PROCESSING, UNKNOWN),
                aTilingTask(PROCESSING, UNKNOWN),
                aTilingTask(FINISHED, FAILED),
                aTilingTask(FINISHED, SUCCEEDED)));
    when(zoneTilingJobRepository.findById(processingJob)).thenReturn(Optional.of(zoneTilingJob));

    var event = new ZTJStatusRecomputingSubmitted(processingJob, 180L);
    event.setAttemptNb(9);
    subject.accept(event);

    verify(taskStatusServiceMock, times(2)).fail(any());
    var jobCaptor = ArgumentCaptor.forClass(ZoneTilingJob.class);
    verify(tilingJobServiceMock, times(1)).recomputeStatus(jobCaptor.capture());
    assertFalse(jobCaptor.getValue().isFailed());
  }

  private static ParcelTilingTask aTilingTask(
      ProgressionStatus progressionStatus, HealthStatus healthStatus) {
    return ParcelTilingTask.builder()
        .id(randomUUID().toString())
        .statusHistory(
            List.of(
                TaskStatus.builder()
                    .progression(progressionStatus)
                    .health(healthStatus)
                    .creationDatetime(now())
                    .build()))
        .build();
  }

  private static ZoneTilingJob aZTJ(
      String jobId, ProgressionStatus progressionStatus, HealthStatus healthStatus) {
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
}
