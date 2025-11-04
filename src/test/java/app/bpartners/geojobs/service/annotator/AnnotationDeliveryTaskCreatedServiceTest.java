package app.bpartners.geojobs.service.annotator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

import app.bpartners.geojobs.endpoint.event.EventProducer;
import app.bpartners.geojobs.endpoint.event.model.annotation.AnnotationDeliveryTaskCreated;
import app.bpartners.geojobs.endpoint.event.model.annotation.AnnotationDeliveryTaskSucceeded;
import app.bpartners.geojobs.job.service.TaskStatusService;
import app.bpartners.geojobs.repository.model.annotation.AnnotationDeliveryTask;
import app.bpartners.geojobs.service.event.AnnotationDeliveryTaskConsumer;
import app.bpartners.geojobs.service.event.AnnotationDeliveryTaskCreatedService;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class AnnotationDeliveryTaskCreatedServiceTest {
  TaskStatusService<AnnotationDeliveryTask> taskStatusServiceMock = mock();
  AnnotationDeliveryTaskConsumer deliveryTaskConsumerMock = mock();
  EventProducer eventProducerMock = mock();
  AnnotationDeliveryTaskCreatedService subject =
      new AnnotationDeliveryTaskCreatedService(
          taskStatusServiceMock, deliveryTaskConsumerMock, eventProducerMock);

  @Test
  void accept_ok() {
    AnnotationDeliveryTask deliveryTask = new AnnotationDeliveryTask();
    AnnotationDeliveryTaskCreated event =
        AnnotationDeliveryTaskCreated.builder().deliveryTask(deliveryTask).build();

    subject.accept(event);

    var listCaptor = ArgumentCaptor.forClass(List.class);
    verify(taskStatusServiceMock, only()).process(deliveryTask);
    verify(deliveryTaskConsumerMock, only()).accept(deliveryTask);
    verify(eventProducerMock, only()).accept(listCaptor.capture());
    var deliveryTaskSucceeded =
        ((List<AnnotationDeliveryTaskSucceeded>) listCaptor.getValue()).getFirst();
    var retrievedTask = deliveryTaskSucceeded.getDeliveryTask();
    assertEquals(
        AnnotationDeliveryTaskSucceeded.builder()
            .deliveryTask(
                deliveryTask.toBuilder().statusHistory(retrievedTask.getStatusHistory()).build())
            .build(),
        deliveryTaskSucceeded);
    assertEquals(Duration.ofMinutes(10), deliveryTaskSucceeded.maxConsumerDuration());
    assertEquals(Duration.ofMinutes(1), deliveryTaskSucceeded.maxConsumerBackoffBetweenRetries());
    assertTrue(retrievedTask.isSucceeded());
  }
}
