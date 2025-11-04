package app.bpartners.geojobs.job.service;

import static app.bpartners.geojobs.job.model.Status.HealthStatus.FAILED;
import static app.bpartners.geojobs.job.model.Status.HealthStatus.SUCCEEDED;
import static app.bpartners.geojobs.job.model.Status.HealthStatus.UNKNOWN;
import static app.bpartners.geojobs.job.model.Status.ProgressionStatus.FINISHED;
import static app.bpartners.geojobs.job.model.Status.ProgressionStatus.PROCESSING;
import static java.time.Instant.now;

import app.bpartners.geojobs.job.model.Status.HealthStatus;
import app.bpartners.geojobs.job.model.Status.ProgressionStatus;
import app.bpartners.geojobs.job.model.Task;
import app.bpartners.geojobs.job.model.TaskStatus;
import app.bpartners.geojobs.job.repository.TaskStatusRepository;
import lombok.AllArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

@AllArgsConstructor
public class TaskStatusService<T extends Task> {

  protected final TaskStatusRepository taskStatusRepository;

  @Transactional
  public T process(T task) {
    return update(task, PROCESSING, UNKNOWN);
  }

  @Transactional
  public T succeed(T task) {
    return update(task, FINISHED, SUCCEEDED);
  }

  @Transactional
  public T fail(T task) {
    return update(task, FINISHED, FAILED);
  }

  private T update(T task, ProgressionStatus progression, HealthStatus health) {
    var taskStatus =
        TaskStatus.builder()
            .creationDatetime(now())
            .progression(progression)
            .health(health)
            .taskId(task.getId())
            .build();
    task.hasNewStatus(taskStatus);
    taskStatusRepository.save(taskStatus);

    return task;
  }
}
