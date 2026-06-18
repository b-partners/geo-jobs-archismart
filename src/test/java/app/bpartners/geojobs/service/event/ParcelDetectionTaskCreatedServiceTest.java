package app.bpartners.geojobs.service.event;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import app.bpartners.geojobs.endpoint.event.model.parcel.ParcelDetectionTaskCreated;
import app.bpartners.geojobs.job.repository.TaskRepository;
import app.bpartners.geojobs.job.service.TaskStatusService;
import app.bpartners.geojobs.repository.model.detection.ParcelDetectionTask;
import org.junit.jupiter.api.Test;

class ParcelDetectionTaskCreatedServiceTest {
  ParcelDetectionTaskConsumer taskConsumerMock = mock();
  TaskStatusService<ParcelDetectionTask> taskStatusServiceMock = mock();
  TaskRepository<ParcelDetectionTask> taskRepositoryMock = mock();
  ParcelDetectionTaskCreatedService subject =
      new ParcelDetectionTaskCreatedService(
          taskConsumerMock, taskStatusServiceMock, taskRepositoryMock);

  @Test
  void processes_task_without_succeeding_on_first_attempt() {
    var taskMock = mock(ParcelDetectionTask.class);
    var event = mock(ParcelDetectionTaskCreated.class);
    when(event.getAttemptNb()).thenReturn(1);
    when(event.getTask()).thenReturn(taskMock);

    assertDoesNotThrow(() -> subject.accept(event));

    verify(taskStatusServiceMock).process(taskMock);
    verify(taskConsumerMock).accept(taskMock);
    // terminal status is driven by the sub ParcelDetectionJob recompute, never here
    verify(taskStatusServiceMock, never()).succeed(taskMock);
    verify(taskStatusServiceMock, never()).fail(taskMock);
  }

  @Test
  void does_not_fail_when_consumer_succeeds() {
    var taskMock = mock(ParcelDetectionTask.class);
    var event = mock(ParcelDetectionTaskCreated.class);
    when(event.getAttemptNb()).thenReturn(3);
    when(event.getTask()).thenReturn(taskMock);

    assertDoesNotThrow(() -> subject.accept(event));

    verify(taskStatusServiceMock, never()).process(taskMock);
    verify(taskConsumerMock).accept(taskMock);
    verify(taskStatusServiceMock, never()).fail(taskMock);
    verify(taskStatusServiceMock, never()).succeed(taskMock);
  }

  @Test
  void fails_task_directly_without_retry_when_consumer_throws() {
    var taskMock = mock(ParcelDetectionTask.class);
    var event = mock(ParcelDetectionTaskCreated.class);
    when(event.getAttemptNb()).thenReturn(1);
    when(event.getTask()).thenReturn(taskMock);
    doThrow(new IllegalArgumentException("parcel without tiles"))
        .when(taskConsumerMock)
        .accept(taskMock);

    // fail-fast: the exception is swallowed (no rethrow) so the message is acked, not retried
    assertDoesNotThrow(() -> subject.accept(event));

    verify(taskConsumerMock).accept(taskMock);
    verify(taskRepositoryMock).save(taskMock);
    verify(taskStatusServiceMock).fail(taskMock);
    verify(taskStatusServiceMock, never()).succeed(taskMock);
  }
}
