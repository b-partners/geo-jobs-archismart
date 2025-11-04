package app.bpartners.geojobs.service.annotator;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import app.bpartners.geojobs.model.exception.ApiException;
import app.bpartners.geojobs.repository.model.annotation.AnnotationDeliveryTask;
import app.bpartners.geojobs.service.event.AnnotationDeliveryTaskConsumer;
import org.junit.jupiter.api.Test;

class AnnotationDeliveryTaskConsumerTest {
  AnnotationService annotationServiceMock = mock();
  AnnotationDeliveryTaskConsumer subject =
      new AnnotationDeliveryTaskConsumer(annotationServiceMock);

  @Test
  void consumes_delivery_task_ok() {
    doNothing().when(annotationServiceMock).addAnnotationTask(any(), any());
    var task = new AnnotationDeliveryTask();

    subject.accept(task);

    verify(annotationServiceMock, only()).addAnnotationTask(any(), any());
  }

  @Test
  void consumes_delivery_task_ko() {
    doThrow(ApiException.class).when(annotationServiceMock).addAnnotationTask(any(), any());
    var task = new AnnotationDeliveryTask();

    assertThrows(ApiException.class, () -> subject.accept(task));

    verify(annotationServiceMock, only()).addAnnotationTask(any(), any());
  }
}
