package app.bpartners.geojobs.endpoint.rest.validator;

import static app.bpartners.geojobs.job.model.Status.ProgressionStatus.PROCESSING;
import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import app.bpartners.geojobs.job.model.JobStatus;
import app.bpartners.geojobs.model.exception.NotFoundException;
import app.bpartners.geojobs.model.exception.NotImplementedException;
import app.bpartners.geojobs.repository.ZoneDetectionJobRepository;
import app.bpartners.geojobs.repository.model.detection.ZoneDetectionJob;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class ZoneDetectionJobValidatorTest {
  public static final String MOCK_JOB_ID = "mock_job_id";
  ZoneDetectionJobRepository jobRepositoryMock = mock();

  ZoneDetectionJobValidator subject = new ZoneDetectionJobValidator(jobRepositoryMock);

  @Test
  void not_found_zdj() {
    when(jobRepositoryMock.findById(MOCK_JOB_ID)).thenReturn(Optional.empty());

    assertThrows(NotFoundException.class, () -> subject.accept(MOCK_JOB_ID));
  }

  @Test
  void human_zdj_not_implemented() {
    when(jobRepositoryMock.findById(MOCK_JOB_ID))
        .thenReturn(
            Optional.of(
                ZoneDetectionJob.builder()
                    .id(MOCK_JOB_ID)
                    .detectionType(ZoneDetectionJob.DetectionType.HUMAN)
                    .build()));

    assertThrows(NotImplementedException.class, () -> subject.accept(MOCK_JOB_ID));
  }

  @Test
  void not_pending_job_progression_not_implemented() {
    when(jobRepositoryMock.findById(MOCK_JOB_ID))
        .thenReturn(
            Optional.of(
                ZoneDetectionJob.builder()
                    .id(MOCK_JOB_ID)
                    .detectionType(ZoneDetectionJob.DetectionType.HUMAN)
                    .statusHistory(List.of(JobStatus.builder().progression(PROCESSING).build()))
                    .build()));

    assertThrows(NotImplementedException.class, () -> subject.accept(MOCK_JOB_ID));
  }
}
