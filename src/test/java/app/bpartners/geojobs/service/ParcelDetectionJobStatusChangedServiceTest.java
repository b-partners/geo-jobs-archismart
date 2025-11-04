package app.bpartners.geojobs.service;

import static app.bpartners.geojobs.job.model.Status.HealthStatus.*;
import static app.bpartners.geojobs.job.model.Status.ProgressionStatus.FINISHED;
import static app.bpartners.geojobs.job.model.Status.ProgressionStatus.PROCESSING;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

import app.bpartners.geojobs.endpoint.event.model.parcel.ParcelDetectionJobStatusChanged;
import app.bpartners.geojobs.job.model.JobStatus;
import app.bpartners.geojobs.job.model.Status;
import app.bpartners.geojobs.job.service.TaskStatusService;
import app.bpartners.geojobs.model.exception.NotFoundException;
import app.bpartners.geojobs.repository.ParcelDetectionTaskRepository;
import app.bpartners.geojobs.repository.model.detection.ParcelDetectionJob;
import app.bpartners.geojobs.repository.model.detection.ParcelDetectionTask;
import app.bpartners.geojobs.service.event.ParcelDetectionJobStatusChangedService;
import java.util.ArrayList;
import java.util.Optional;
import org.junit.jupiter.api.Test;

public class ParcelDetectionJobStatusChangedServiceTest {
  private static final String JOB_ID = "jobId";
  StatusChangedHandler statusChangedHandler = new StatusChangedHandler();
  ParcelDetectionTaskRepository parcelDetectionTaskRepositoryMock = mock();
  TaskStatusService<ParcelDetectionTask> taskStatusServiceMock = mock();
  ParcelDetectionJobStatusChangedService subject =
      new ParcelDetectionJobStatusChangedService(
          statusChangedHandler, parcelDetectionTaskRepositoryMock, taskStatusServiceMock);

  @Test
  void handle_succeeded_job_ok() {
    ParcelDetectionTask parcelDetectionTask = ParcelDetectionTask.builder().build();
    when(parcelDetectionTaskRepositoryMock.findByAsJobId(JOB_ID))
        .thenReturn(Optional.of(parcelDetectionTask));

    subject.accept(
        new ParcelDetectionJobStatusChanged(
            aParcelDetectionJob(JOB_ID, PROCESSING, UNKNOWN),
            aParcelDetectionJob(JOB_ID, FINISHED, SUCCEEDED)));

    verify(parcelDetectionTaskRepositoryMock, times(1)).findByAsJobId(JOB_ID);
    verify(taskStatusServiceMock, times(1)).succeed(parcelDetectionTask);
  }

  @Test
  void accept_ko() {
    when(parcelDetectionTaskRepositoryMock.findByAsJobId(JOB_ID)).thenReturn(Optional.empty());

    assertThrows(
        NotFoundException.class,
        () ->
            subject.accept(
                new ParcelDetectionJobStatusChanged(
                    aParcelDetectionJob(JOB_ID, PROCESSING, UNKNOWN),
                    aParcelDetectionJob(JOB_ID, FINISHED, SUCCEEDED))));

    verify(parcelDetectionTaskRepositoryMock, times(1)).findByAsJobId(JOB_ID);
  }

  @Test
  void handle_failed_job_ok() {
    ParcelDetectionTask parcelDetectionTask = ParcelDetectionTask.builder().build();
    when(parcelDetectionTaskRepositoryMock.findByAsJobId(JOB_ID))
        .thenReturn(Optional.of(parcelDetectionTask));

    subject.accept(
        new ParcelDetectionJobStatusChanged(
            aParcelDetectionJob(JOB_ID, PROCESSING, UNKNOWN),
            aParcelDetectionJob(JOB_ID, FINISHED, FAILED)));

    verify(parcelDetectionTaskRepositoryMock, times(1)).findByAsJobId(JOB_ID);
    verify(taskStatusServiceMock, times(1)).fail(parcelDetectionTask);
  }

  private static ParcelDetectionJob aParcelDetectionJob(
      String id, Status.ProgressionStatus progression, Status.HealthStatus health) {
    var statusHistory = new ArrayList<JobStatus>();
    statusHistory.add(JobStatus.builder().progression(progression).health(health).build());
    return ParcelDetectionJob.builder().id(id).statusHistory(statusHistory).build();
  }
}
