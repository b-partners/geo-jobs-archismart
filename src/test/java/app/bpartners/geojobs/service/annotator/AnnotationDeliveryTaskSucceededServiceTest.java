package app.bpartners.geojobs.service.annotator;

import static org.mockito.Mockito.*;

import app.bpartners.geojobs.endpoint.event.model.annotation.AnnotationDeliveryTaskSucceeded;
import app.bpartners.geojobs.job.service.TaskStatusService;
import app.bpartners.geojobs.repository.AnnotationDeliveryTaskRepository;
import app.bpartners.geojobs.repository.model.annotation.AnnotationDeliveryTask;
import app.bpartners.geojobs.service.event.AnnotationDeliveryTaskSucceededService;
import org.junit.jupiter.api.Test;

class AnnotationDeliveryTaskSucceededServiceTest {
  TaskStatusService<AnnotationDeliveryTask> taskStatusServiceMock = mock();
  AnnotationDeliveryTaskRepository taskRepositoryMock = mock();
  AnnotationDeliveryTaskSucceededService subject =
      new AnnotationDeliveryTaskSucceededService(taskStatusServiceMock, taskRepositoryMock);

  @Test
  void accept_ok() {
    var task = new AnnotationDeliveryTask();

    subject.accept(AnnotationDeliveryTaskSucceeded.builder().deliveryTask(task).build());

    verify(taskRepositoryMock, only()).save(task);
    verify(taskStatusServiceMock, only()).succeed(task);
  }
}
