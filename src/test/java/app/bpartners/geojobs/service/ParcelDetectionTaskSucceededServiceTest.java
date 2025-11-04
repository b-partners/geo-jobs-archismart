package app.bpartners.geojobs.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.*;

import app.bpartners.geojobs.endpoint.event.model.parcel.ParcelDetectionTaskSucceeded;
import app.bpartners.geojobs.job.service.TaskStatusService;
import app.bpartners.geojobs.repository.ParcelDetectionTaskRepository;
import app.bpartners.geojobs.repository.model.detection.ParcelDetectionTask;
import app.bpartners.geojobs.service.event.ParcelDetectionTaskSucceededService;
import org.junit.jupiter.api.Test;

public class ParcelDetectionTaskSucceededServiceTest {
  ParcelDetectionTaskRepository taskRepositoryMock = mock();
  TaskStatusService<ParcelDetectionTask> taskStatusServiceMock = mock();
  ParcelDetectionTaskSucceededService subject =
      new ParcelDetectionTaskSucceededService(taskRepositoryMock, taskStatusServiceMock);

  @Test
  void accept_ok() {
    ParcelDetectionTask parcelDetectionTask = ParcelDetectionTask.builder().build();

    assertDoesNotThrow(() -> subject.accept(new ParcelDetectionTaskSucceeded(parcelDetectionTask)));

    verify(taskRepositoryMock, times(1)).save(parcelDetectionTask);
    verify(taskStatusServiceMock, times(1)).succeed(parcelDetectionTask);
  }
}
