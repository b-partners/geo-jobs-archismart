package app.bpartners.geojobs.service.annotator;

import static app.bpartners.geojobs.job.model.Status.HealthStatus.UNKNOWN;
import static app.bpartners.geojobs.job.model.Status.ProgressionStatus.PENDING;
import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

import app.bpartners.geojobs.endpoint.event.EventProducer;
import app.bpartners.geojobs.endpoint.event.model.annotation.AnnotationDeliveryTaskCreated;
import app.bpartners.geojobs.job.repository.JobStatusRepository;
import app.bpartners.geojobs.job.repository.TaskRepository;
import app.bpartners.geojobs.repository.TaskStatisticRepository;
import app.bpartners.geojobs.repository.model.annotation.AnnotationDeliveryJob;
import app.bpartners.geojobs.repository.model.annotation.AnnotationDeliveryTask;
import app.bpartners.geojobs.service.AnnotationDeliveryJobService;
import app.bpartners.geojobs.utils.annotation.AnnotationDeliveryTaskCreator;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.jpa.repository.JpaRepository;

class AnnotationDeliveryJobServiceTest {
  TaskRepository<AnnotationDeliveryTask> deliveryTaskTaskRepositoryMock = mock();
  JpaRepository<AnnotationDeliveryJob, String> deliveryJobRepositoryMock = mock();
  JobStatusRepository statusRepositoryMock = mock();
  TaskStatisticRepository statisticRepositoryMock = mock();
  EventProducer eventProducerMock = mock();
  AnnotationDeliveryTaskCreator annotationDeliveryTaskCreator = new AnnotationDeliveryTaskCreator();
  AnnotationDeliveryJobService subject =
      new AnnotationDeliveryJobService(
          deliveryJobRepositoryMock,
          statusRepositoryMock,
          statisticRepositoryMock,
          deliveryTaskTaskRepositoryMock,
          eventProducerMock);

  @Test
  void fire_tasks_ok() {
    var jobId = randomUUID().toString();
    var annotationJobId = randomUUID().toString();
    var deliveryJob =
        AnnotationDeliveryJob.builder().id(jobId).annotationJobId(annotationJobId).build();
    var deliveryTask =
        annotationDeliveryTaskCreator.create(
            randomUUID().toString(), jobId, annotationJobId, PENDING, UNKNOWN);
    when(deliveryJobRepositoryMock.findById(jobId)).thenReturn(Optional.of(deliveryJob));
    when(deliveryTaskTaskRepositoryMock.findAllByJobId(jobId)).thenReturn(List.of(deliveryTask));

    subject.fireTasks(deliveryJob);

    var listCaptor = ArgumentCaptor.forClass(List.class);
    verify(eventProducerMock, times(1)).accept(listCaptor.capture());
    var deliveryTaskCreated =
        ((List<AnnotationDeliveryTaskCreated>) listCaptor.getAllValues().getFirst()).getFirst();
    assertEquals(new AnnotationDeliveryTaskCreated(deliveryTask), deliveryTaskCreated);
    assertEquals(Duration.ofMinutes(10L), deliveryTaskCreated.maxConsumerDuration());
    assertEquals(Duration.ofMinutes(1L), deliveryTaskCreated.maxConsumerBackoffBetweenRetries());
  }
}
