package app.bpartners.geojobs.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

import app.bpartners.geojobs.endpoint.event.EventProducer;
import app.bpartners.geojobs.endpoint.event.model.parcel.ParcelDetectionJobStatusChanged;
import app.bpartners.geojobs.job.repository.JobStatusRepository;
import app.bpartners.geojobs.job.repository.TaskRepository;
import app.bpartners.geojobs.repository.TaskStatisticRepository;
import app.bpartners.geojobs.repository.model.TileDetectionTask;
import app.bpartners.geojobs.repository.model.detection.ParcelDetectionJob;
import app.bpartners.geojobs.service.detection.ParcelDetectionJobService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.jpa.repository.JpaRepository;

class ParcelDetectionJobServiceTest {
  JpaRepository<ParcelDetectionJob, String> parcelDetectionJobRepositoryMock = mock();
  TaskStatisticRepository taskStatisticRepositoryMock = mock();
  JobStatusRepository jobStatusRepositoryMock = mock();
  TaskRepository<TileDetectionTask> taskRepositoryMock = mock();
  EventProducer eventProducerMock = mock();
  ParcelDetectionJobService subject =
      new ParcelDetectionJobService(
          parcelDetectionJobRepositoryMock,
          taskStatisticRepositoryMock,
          jobStatusRepositoryMock,
          taskRepositoryMock,
          eventProducerMock);

  @Test
  void on_status_changed_ok() {
    var expected =
        new ParcelDetectionJobStatusChanged(new ParcelDetectionJob(), new ParcelDetectionJob());

    assertDoesNotThrow(
        () -> subject.onStatusChanged(new ParcelDetectionJob(), new ParcelDetectionJob()));

    var listCaptor = ArgumentCaptor.forClass(List.class);
    verify(eventProducerMock, times(1)).accept(listCaptor.capture());
    var statusChangedEvent =
        ((List<ParcelDetectionJobStatusChanged>) listCaptor.getValue()).getFirst();
    assertEquals(expected, statusChangedEvent);
  }
}
