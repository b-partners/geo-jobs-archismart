package app.bpartners.geojobs.service.event;

import static app.bpartners.geojobs.job.model.Status.HealthStatus.SUCCEEDED;
import static app.bpartners.geojobs.job.model.Status.ProgressionStatus.FINISHED;
import static app.bpartners.geojobs.service.event.AnnotationDeliveryTaskConsumer.withNewStatus;

import app.bpartners.geojobs.endpoint.event.EventProducer;
import app.bpartners.geojobs.endpoint.event.model.annotation.AnnotationDeliveryTaskCreated;
import app.bpartners.geojobs.endpoint.event.model.annotation.AnnotationDeliveryTaskSucceeded;
import app.bpartners.geojobs.job.service.TaskStatusService;
import app.bpartners.geojobs.repository.model.annotation.AnnotationDeliveryTask;
import java.util.List;
import java.util.function.Consumer;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
@Slf4j
public class AnnotationDeliveryTaskCreatedService
    implements Consumer<AnnotationDeliveryTaskCreated> {
  private final TaskStatusService<AnnotationDeliveryTask> taskStatusService;
  private final AnnotationDeliveryTaskConsumer consumer;
  private final EventProducer eventProducer;

  @Override
  public void accept(AnnotationDeliveryTaskCreated event) {
    var task = event.getDeliveryTask();
    try {
      taskStatusService.process(task);
    } catch (IllegalArgumentException e) {
      // TODO: find why duplicated task created
      var errorMsg = e.getMessage();
      if (errorMsg.contains("old=Status{progression=FINISHED")
          && errorMsg.contains("new=Status{progression=PROCESSING")) {
        log.error(errorMsg);
        // skip and do nothing
        return;
      }
    }

    consumer.accept(task);

    eventProducer.accept(
        List.of(
            AnnotationDeliveryTaskSucceeded.builder()
                .deliveryTask(withNewStatus(task, FINISHED, SUCCEEDED, null))
                .build()));
  }
}
