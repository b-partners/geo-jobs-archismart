package app.bpartners.geojobs.service.event;

import static app.bpartners.geojobs.model.exception.ApiException.ExceptionType.SERVER_EXCEPTION;
import static java.time.Instant.now;

import app.bpartners.geojobs.job.model.Status;
import app.bpartners.geojobs.model.exception.ApiException;
import app.bpartners.geojobs.repository.model.annotation.AnnotationDeliveryTask;
import app.bpartners.geojobs.service.annotator.AnnotationService;
import java.util.function.Consumer;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
@Slf4j
public class AnnotationDeliveryTaskConsumer implements Consumer<AnnotationDeliveryTask> {
  private final AnnotationService annotationService;

  @Override
  public void accept(AnnotationDeliveryTask task) {
    var annotationJobId = task.getAnnotationJobId();
    var annotatedTask = task.getCreateAnnotatedTask();
    log.info("DEBUG Annotated task: {}", annotatedTask);

    try {
      annotationService.addAnnotationTask(annotationJobId, annotatedTask);
    } catch (Exception e) {
      throw new ApiException(SERVER_EXCEPTION, e);
    }
  }

  public static AnnotationDeliveryTask withNewStatus(
      AnnotationDeliveryTask task,
      Status.ProgressionStatus progression,
      Status.HealthStatus health,
      String message) {
    return (AnnotationDeliveryTask)
        task.hasNewStatus(
            Status.builder()
                .progression(progression)
                .health(health)
                .creationDatetime(now())
                .message(message)
                .build());
  }
}
