package app.bpartners.geojobs.service.event;

import app.bpartners.geojobs.endpoint.event.EventProducer;
import app.bpartners.geojobs.endpoint.event.model.annotation.AnnotationRetrievingJobStatusRecomputingSubmitted;
import app.bpartners.geojobs.endpoint.event.model.annotation.AnnotationRetrievingTaskSucceeded;
import app.bpartners.geojobs.job.service.TaskStatusService;
import app.bpartners.geojobs.repository.AnnotationRetrievingTaskRepository;
import app.bpartners.geojobs.repository.model.annotation.AnnotationRetrievingTask;
import java.util.List;
import java.util.function.Consumer;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class AnnotationRetrievingTaskSucceededService
    implements Consumer<AnnotationRetrievingTaskSucceeded> {
  private final TaskStatusService<AnnotationRetrievingTask> taskStatusService;
  private final AnnotationRetrievingTaskRepository repository;
  private final EventProducer eventProducer;

  @Override
  public void accept(AnnotationRetrievingTaskSucceeded event) {
    var task = event.getAnnotationRetrievingTask();
    repository.save(task);
    taskStatusService.succeed(task);

    eventProducer.accept(
        List.of(new AnnotationRetrievingJobStatusRecomputingSubmitted(task.getJobId())));
  }
}
