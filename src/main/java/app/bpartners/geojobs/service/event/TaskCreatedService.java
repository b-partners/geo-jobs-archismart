package app.bpartners.geojobs.service.event;

import app.bpartners.geojobs.endpoint.event.model.TaskCreated;
import app.bpartners.geojobs.job.model.Task;
import app.bpartners.geojobs.job.repository.TaskRepository;
import app.bpartners.geojobs.job.service.TaskStatusService;
import app.bpartners.geojobs.model.exception.ImageSourcesTimeoutException;
import app.bpartners.geojobs.service.TaskConsumer;
import java.util.function.Consumer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class TaskCreatedService<T extends Task, C extends TaskCreated<T>> implements Consumer<C> {
  protected static final int MAX_ATTEMPT_NB = 5;
  private final TaskConsumer<T> taskConsumer;
  protected final TaskStatusService<T> taskStatusService;
  private final TaskRepository<T> taskRepository;

  protected boolean isRetryable() {
    return false;
  }

  @Override
  public void accept(C event) {
    var task = event.getTask();
    var attemptNb = event.getAttemptNb();

    if (attemptNb == 1) {
      taskStatusService.process(task);
    }

    // /!\ This only triggers if the queue's redrive maxReceiveCount is >= MAX_ATTEMPT_NB.
    if (isRetryable() && attemptNb >= MAX_ATTEMPT_NB) {
      log.error(
          "Task [{} - id={}] reached max attempt {}/{}, marking it as FAILED",
          task,
          task.getId(),
          attemptNb,
          MAX_ATTEMPT_NB);
      fail(task);
      return;
    }

    try {
      taskConsumer.accept(task);
    } catch (RuntimeException e) {
      var exceptionType = e.getClass();
      if (isRetryable() && !exceptionType.equals(ImageSourcesTimeoutException.class)) {
        log.error(
            "Task [{} - id={}] failed at attempt {}/{}, it will be retried",
            task,
            task.getId(),
            attemptNb,
            MAX_ATTEMPT_NB,
            e);
        throw e; // not acked -> SQS redelivers until attemptNb reaches MAX_ATTEMPT_NB
      }
      log.error(
          "Task [{} - id={}] failed, marking it as FAILED without retry", task, task.getId(), e);
      fail(task);
      return;
    }

    succeed(task);
  }

  protected void succeed(T task) {
    taskRepository.save(task);
    taskStatusService.succeed(task);
  }

  protected void fail(T task) {
    taskRepository.save(task);
    taskStatusService.fail(task);
  }
}
