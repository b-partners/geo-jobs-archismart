package app.bpartners.geojobs.service.annotator;

import static app.bpartners.geojobs.job.model.Status.HealthStatus.UNKNOWN;
import static app.bpartners.geojobs.job.model.Status.ProgressionStatus.PENDING;
import static app.bpartners.geojobs.repository.model.GeoJobType.ANNOTATION_DELIVERY;
import static java.time.Instant.now;
import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import app.bpartners.gen.annotator.endpoint.rest.model.Label;
import app.bpartners.geojobs.endpoint.event.EventProducer;
import app.bpartners.geojobs.repository.model.annotation.AnnotationDeliveryJob;
import app.bpartners.geojobs.service.AnnotationDeliveryJobService;
import app.bpartners.geojobs.service.detection.ZoneDetectionJobService;
import app.bpartners.geojobs.sqs.EventProducerInvocationMock;
import app.bpartners.geojobs.sqs.LocalEventQueue;
import app.bpartners.geojobs.utils.annotation.AnnotationDeliveryTaskCreator;
import app.bpartners.geojobs.utils.detection.DetectionIT;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

class AnnotationDeliveryJobCreatedIT extends DetectionIT {
  private static final int DEFAULT_EVENT_DELAY_SPEED_FACTOR = 10;
  @Autowired ZoneDetectionJobService detectionJobService;
  @MockBean EventProducer eventProducerMock;
  @MockBean AnnotationService annotationServiceMock;
  @Autowired AnnotationDeliveryJobService deliveryJobService;
  @Autowired LocalEventQueue localEventQueue;
  EventProducerInvocationMock eventProducerInvocationMock = new EventProducerInvocationMock();
  AnnotationDeliveryTaskCreator annotationDeliveryTaskCreator = new AnnotationDeliveryTaskCreator();

  @BeforeEach
  void setUp() {
    localEventQueue.configure(new ArrayList<>(), DEFAULT_EVENT_DELAY_SPEED_FACTOR);
    // Set random failure if necessary here
    doNothing().when(annotationServiceMock).saveAnnotationJob(any(), any(), any(), any(), any());
    doNothing().when(annotationServiceMock).addAnnotationTask(any(), any());
    doAnswer(
            invocationOnMock ->
                eventProducerInvocationMock.apply(localEventQueue, invocationOnMock))
        .when(eventProducerMock)
        .accept(any());
  }

  @SneakyThrows
  @Test
  @Disabled
  void single_event_succeeded() {
    var tilingJob = finishedZoneTilingJob(randomUUID().toString());
    var zdj =
        detectionJobService.save(succeededZoneDetectionJob(randomUUID().toString(), tilingJob));
    var deliveryJobId = randomUUID().toString();
    var deliveryJob =
        AnnotationDeliveryJob.builder()
            .id(deliveryJobId)
            .annotationJobId(randomUUID().toString())
            .annotationJobName("dummyDeliveryJobName")
            .detectionJobId(zdj.getId())
            .labels(List.of(new Label().id(randomUUID().toString()).name("PISCINE").color("#000")))
            .submissionInstant(now())
            .build();
    deliveryJob.hasNewStatus(
        app.bpartners.geojobs.job.model.JobStatus.builder()
            .id(randomUUID().toString())
            .jobId(deliveryJobId)
            .creationDatetime(now())
            .jobType(ANNOTATION_DELIVERY)
            .progression(PENDING)
            .health(UNKNOWN)
            .build());
    int tasksNb = 10;
    var deliveryTasks =
        annotationDeliveryTaskCreator.someDeliveryTasks(
            tasksNb, deliveryJob.getId(), deliveryJob.getAnnotationJobId(), PENDING, UNKNOWN);

    var savedDeliveryJob = deliveryJobService.create(deliveryJob, deliveryTasks);

    Thread.sleep(Duration.ofSeconds(30L));
    if (localEventQueue != null) localEventQueue.attemptSchedulerShutDown();
    var retrievedDeliveryJob = deliveryJobService.findById(savedDeliveryJob.getId());

    assertTrue(retrievedDeliveryJob.isSucceeded());
  }
}
