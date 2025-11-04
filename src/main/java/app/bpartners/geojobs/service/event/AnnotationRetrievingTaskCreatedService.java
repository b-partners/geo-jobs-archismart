package app.bpartners.geojobs.service.event;

import static app.bpartners.geojobs.job.model.Status.HealthStatus.SUCCEEDED;
import static app.bpartners.geojobs.job.model.Status.ProgressionStatus.FINISHED;
import static app.bpartners.geojobs.service.event.AnnotationRetrievingTaskConsumer.withNewStatus;

import app.bpartners.geojobs.endpoint.event.EventProducer;
import app.bpartners.geojobs.endpoint.event.model.annotation.AnnotationRetrievingTaskCreated;
import app.bpartners.geojobs.endpoint.event.model.annotation.AnnotationRetrievingTaskSucceeded;
import app.bpartners.geojobs.job.service.TaskStatusService;
import app.bpartners.geojobs.repository.model.annotation.AnnotationRetrievingTask;
import java.util.List;
import java.util.function.Consumer;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
@Slf4j
public class AnnotationRetrievingTaskCreatedService
    implements Consumer<AnnotationRetrievingTaskCreated> {
  private final TaskStatusService<AnnotationRetrievingTask> taskStatusService;
  private final AnnotationRetrievingTaskConsumer consumer;
  private final EventProducer eventProducer;

  @Override
  public void accept(AnnotationRetrievingTaskCreated event) {
    var task = event.getAnnotationRetrievingTask();
    taskStatusService.process(task);

    consumer.accept(task);

    eventProducer.accept(
        List.of(
            AnnotationRetrievingTaskSucceeded.builder()
                .annotationRetrievingTask(withNewStatus(task, FINISHED, SUCCEEDED, null))
                .build()));
  }
}
