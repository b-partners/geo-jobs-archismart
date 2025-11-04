package app.bpartners.geojobs.utils.annotation;

import static app.bpartners.geojobs.repository.model.GeoJobType.ANNOTATION_DELIVERY;
import static java.time.Instant.now;
import static java.util.UUID.randomUUID;

import app.bpartners.gen.annotator.endpoint.rest.model.CreateAnnotatedTask;
import app.bpartners.geojobs.job.model.Status;
import app.bpartners.geojobs.job.model.TaskStatus;
import app.bpartners.geojobs.repository.model.annotation.AnnotationDeliveryTask;
import java.util.ArrayList;
import java.util.List;

public class AnnotationDeliveryTaskCreator {
  public AnnotationDeliveryTask create(
      String id,
      String jobId,
      String annotationJobId,
      Status.ProgressionStatus progressionStatus,
      Status.HealthStatus healthStatus) {
    return AnnotationDeliveryTask.builder()
        .id(id)
        .annotationJobId(annotationJobId)
        .jobId(jobId)
        .createAnnotatedTask(new CreateAnnotatedTask())
        .submissionInstant(now())
        .statusHistory(
            List.of(
                TaskStatus.builder()
                    .id(randomUUID().toString())
                    .taskId(id)
                    .progression(progressionStatus)
                    .health(healthStatus)
                    .creationDatetime(now())
                    .jobType(ANNOTATION_DELIVERY)
                    .build()))
        .build();
  }

  public List<AnnotationDeliveryTask> someDeliveryTasks(
      Integer nb,
      String jobId,
      String annotationJobId,
      Status.ProgressionStatus progressionStatus,
      Status.HealthStatus healthStatus) {
    var tasks = new ArrayList<AnnotationDeliveryTask>();
    for (int i = 0; i < nb; i++) {
      tasks.add(
          create(randomUUID().toString(), jobId, annotationJobId, progressionStatus, healthStatus));
    }
    return tasks;
  }
}
