package app.bpartners.geojobs.service;

import static app.bpartners.geojobs.job.model.Status.HealthStatus.SUCCEEDED;
import static app.bpartners.geojobs.job.model.Status.ProgressionStatus.FINISHED;
import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import app.bpartners.geojobs.endpoint.event.model.status.ParcelDetectionStatusRecomputingSubmitted;
import app.bpartners.geojobs.job.model.JobStatus;
import app.bpartners.geojobs.job.model.Status;
import app.bpartners.geojobs.repository.model.detection.ParcelDetectionJob;
import app.bpartners.geojobs.service.detection.ParcelDetectionJobService;
import app.bpartners.geojobs.service.event.ParcelDetectionStatusRecomputingSubmittedService;
import java.util.List;
import org.junit.jupiter.api.Test;

class ParcelDetectionStatusRecomputingSubmittedServiceTest {
  private static final String JOB_ID = "jobId";
  ParcelDetectionJobService parcelDetectionJobServiceMock = mock();
  ParcelDetectionStatusRecomputingSubmittedService subject =
      new ParcelDetectionStatusRecomputingSubmittedService(
          parcelDetectionJobServiceMock, mock(), mock());

  @Test
  void job_with_finished_status_does_not_throw() {
    var finishedPDJ = aPDJ(JOB_ID, FINISHED, SUCCEEDED);
    when(parcelDetectionJobServiceMock.findById(JOB_ID)).thenReturn(finishedPDJ);

    assertDoesNotThrow(() -> subject.accept(new ParcelDetectionStatusRecomputingSubmitted(JOB_ID)));

    verify(parcelDetectionJobServiceMock, times(1)).findById(JOB_ID);
    verify(parcelDetectionJobServiceMock, times(0)).recomputeStatus(finishedPDJ);
  }

  private ParcelDetectionJob aPDJ(
      String jobId, Status.ProgressionStatus progressionStatus, Status.HealthStatus healthStatus) {
    return ParcelDetectionJob.builder()
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
