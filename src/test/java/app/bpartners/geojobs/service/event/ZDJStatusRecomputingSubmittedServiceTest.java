package app.bpartners.geojobs.service.event;

import static app.bpartners.geojobs.job.model.Status.HealthStatus.UNKNOWN;
import static app.bpartners.geojobs.job.model.Status.ProgressionStatus.FINISHED;
import static app.bpartners.geojobs.job.model.Status.ProgressionStatus.PROCESSING;
import static app.bpartners.geojobs.repository.model.detection.ZoneDetectionJob.DetectionType.MACHINE;
import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import app.bpartners.geojobs.endpoint.event.model.status.ZDJStatusRecomputingSubmitted;
import app.bpartners.geojobs.job.model.JobStatus;
import app.bpartners.geojobs.job.model.Status.HealthStatus;
import app.bpartners.geojobs.job.model.Status.ProgressionStatus;
import app.bpartners.geojobs.repository.model.detection.ZoneDetectionJob;
import app.bpartners.geojobs.service.detection.ZoneDetectionJobService;
import app.bpartners.geojobs.service.event.detection.ZDJStatusRecomputingSubmittedBean;
import java.util.List;
import org.junit.jupiter.api.Test;

class ZDJStatusRecomputingSubmittedServiceTest {
  ZoneDetectionJobService jobServiceMock = mock();
  ZDJStatusRecomputingSubmittedBean zdjStatusRecomputingSubmittedBeanMock =
      new ZDJStatusRecomputingSubmittedBean(jobServiceMock, mock(), mock());
  ZDJStatusRecomputingSubmittedService subject =
      new ZDJStatusRecomputingSubmittedService(zdjStatusRecomputingSubmittedBeanMock);

  @Test
  void processing_after_recomputing_throws_exception() {
    var jobId = "jobId";
    var job = aZDJ(jobId, PROCESSING, UNKNOWN);
    when(jobServiceMock.findById(jobId)).thenReturn(job);
    when(jobServiceMock.recomputeStatus(job)).thenReturn(job);

    assertThrows(
        RuntimeException.class, () -> subject.accept(new ZDJStatusRecomputingSubmitted(jobId)));
    verify(jobServiceMock, times(1)).recomputeStatus(job);
  }

  @Test
  void finished_after_recomputing_does_not_throw_exception() {
    var jobId = "jobId";
    var job = aZDJ(jobId, FINISHED, UNKNOWN);
    when(jobServiceMock.findById(jobId)).thenReturn(job);
    when(jobServiceMock.recomputeStatus(job)).thenReturn(job);

    subject.accept(new ZDJStatusRecomputingSubmitted(jobId));
    verify(jobServiceMock, times(0)).recomputeStatus(job);
  }

  private static ZoneDetectionJob aZDJ(
      String jobId, ProgressionStatus progressionStatus, HealthStatus healthStatus) {
    return ZoneDetectionJob.builder()
        .id(jobId)
        .zoneName("dummy")
        .emailReceiver("dummy")
        .detectionType(MACHINE)
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
