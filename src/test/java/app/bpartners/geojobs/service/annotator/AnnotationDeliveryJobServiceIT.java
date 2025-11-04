package app.bpartners.geojobs.service.annotator;

import static app.bpartners.geojobs.job.model.Status.HealthStatus.SUCCEEDED;
import static app.bpartners.geojobs.job.model.Status.HealthStatus.UNKNOWN;
import static app.bpartners.geojobs.job.model.Status.ProgressionStatus.FINISHED;
import static app.bpartners.geojobs.job.model.Status.ProgressionStatus.PENDING;
import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

import app.bpartners.geojobs.conf.FacadeIT;
import app.bpartners.geojobs.endpoint.event.EventProducer;
import app.bpartners.geojobs.endpoint.event.model.annotation.AnnotationDeliveryJobCreated;
import app.bpartners.geojobs.endpoint.event.model.annotation.AnnotationDeliveryJobStatusRecomputingSubmitted;
import app.bpartners.geojobs.endpoint.event.model.annotation.AnnotationDeliveryTaskCreated;
import app.bpartners.geojobs.job.model.Status;
import app.bpartners.geojobs.repository.AnnotationDeliveryJobRepository;
import app.bpartners.geojobs.repository.AnnotationDeliveryTaskRepository;
import app.bpartners.geojobs.repository.ZoneDetectionJobRepository;
import app.bpartners.geojobs.repository.model.annotation.AnnotationDeliveryJob;
import app.bpartners.geojobs.repository.model.annotation.AnnotationDeliveryTask;
import app.bpartners.geojobs.repository.model.detection.ZoneDetectionJob;
import app.bpartners.geojobs.service.AnnotationDeliveryJobService;
import app.bpartners.geojobs.utils.annotation.AnnotationDeliveryTaskCreator;
import app.bpartners.geojobs.utils.detection.ZoneDetectionJobCreator;
import app.bpartners.geojobs.utils.tiling.ZoneTilingJobCreator;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import lombok.NonNull;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

class AnnotationDeliveryJobServiceIT extends FacadeIT {
  @MockBean EventProducer eventProducer;
  @Autowired AnnotationDeliveryJobService subject;
  @Autowired AnnotationDeliveryJobRepository jobRepository;
  @Autowired AnnotationDeliveryTaskRepository taskRepository;
  @Autowired ZoneDetectionJobRepository zoneDetectionJobRepository;
  ZoneDetectionJobCreator zoneDetectionJobCreator = new ZoneDetectionJobCreator();
  ZoneTilingJobCreator zoneTilingJobCreator = new ZoneTilingJobCreator();
  AnnotationDeliveryTaskCreator annotationDeliveryTaskCreator = new AnnotationDeliveryTaskCreator();

  @Test
  void create_ok() {
    var zdj = randomZDJ();
    var jobId = randomUUID().toString();
    var deliveryJob =
        AnnotationDeliveryJob.builder()
            .id(jobId)
            .annotationJobId(randomUUID().toString())
            .annotationJobName("dummyJobName")
            .detectionJobId(zdj.getId())
            .build();
    int tasksNb = 10;
    var deliveryTasks =
        someDeliveryTasks(
            tasksNb, deliveryJob.getId(), deliveryJob.getAnnotationJobId(), PENDING, UNKNOWN);

    var actual = subject.create(deliveryJob, deliveryTasks);

    var listCaptor = ArgumentCaptor.forClass(List.class);
    verify(eventProducer, times(2)).accept(listCaptor.capture());
    List<List> allValues = listCaptor.getAllValues();
    var deliveryJobCreated = ((List<AnnotationDeliveryJobCreated>) allValues.getFirst()).getFirst();
    AnnotationDeliveryJobStatusRecomputingSubmitted jobStatusRecomputingSubmitted =
        (AnnotationDeliveryJobStatusRecomputingSubmitted) allValues.getLast().getFirst();
    assertEquals(
        AnnotationDeliveryJobCreated.builder().deliveryJob(actual).build(), deliveryJobCreated);
    assertEquals(deliveryJob, actual);
    assertEquals(Duration.ofMinutes(3L), deliveryJobCreated.maxConsumerDuration());
    assertEquals(Duration.ofMinutes(3L), deliveryJobCreated.maxConsumerBackoffBetweenRetries());
    assertEquals(
        new AnnotationDeliveryJobStatusRecomputingSubmitted(jobId), jobStatusRecomputingSubmitted);
  }

  @Test
  void fire_tasks_ok() {
    var zdj = randomZDJ();
    var jobId = randomUUID().toString();
    var deliveryJob =
        jobRepository.save(
            AnnotationDeliveryJob.builder()
                .id(jobId)
                .annotationJobId(randomUUID().toString())
                .annotationJobName("dummyJobName")
                .detectionJobId(zdj.getId())
                .build());
    int tasksNb = 10;
    taskRepository.saveAll(
        someDeliveryTasks(
            tasksNb, deliveryJob.getId(), deliveryJob.getAnnotationJobId(), PENDING, UNKNOWN));

    subject.fireTasks(deliveryJob);

    var listCaptor = ArgumentCaptor.forClass(List.class);
    verify(eventProducer, times(tasksNb)).accept(listCaptor.capture());
    List<List> allValues = listCaptor.getAllValues();
    List<List> deliveryTasksCreated = allValues.subList(0, allValues.size() - 1);

    assertTrue(
        deliveryTasksCreated.stream()
            .allMatch(
                list -> list.getFirst().getClass().equals(AnnotationDeliveryTaskCreated.class)));
    assertTrue(
        deliveryTasksCreated.stream()
            .allMatch(
                list -> {
                  var deliveryTaskCreated = (AnnotationDeliveryTaskCreated) list.getFirst();
                  return deliveryTaskCreated.getDeliveryTask() != null;
                }));
  }

  private List<AnnotationDeliveryTask> someDeliveryTasks(
      Integer nb,
      String jobId,
      String annotationJobId,
      Status.ProgressionStatus progressionStatus,
      Status.HealthStatus healthStatus) {
    var tasks = new ArrayList<AnnotationDeliveryTask>();
    for (int i = 0; i < nb; i++) {
      tasks.add(
          annotationDeliveryTaskCreator.create(
              randomUUID().toString(), jobId, annotationJobId, progressionStatus, healthStatus));
    }
    return tasks;
  }

  @NonNull
  private ZoneDetectionJob randomZDJ() {
    var dummyZone = "dummyZone";
    var emailReceiver = "dummy@email.com";
    var ztj =
        zoneTilingJobCreator.create(
            randomUUID().toString(), dummyZone, emailReceiver, FINISHED, SUCCEEDED);
    var zdj =
        zoneDetectionJobCreator.create(
            randomUUID().toString(), dummyZone, emailReceiver, FINISHED, SUCCEEDED, ztj);
    return zoneDetectionJobRepository.save(zdj);
  }
}
