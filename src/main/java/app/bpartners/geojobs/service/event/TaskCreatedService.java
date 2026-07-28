package app.bpartners.geojobs.service.event;

import app.bpartners.geojobs.endpoint.event.model.TaskCreated;
import app.bpartners.geojobs.job.model.Task;
import app.bpartners.geojobs.job.repository.TaskRepository;
import app.bpartners.geojobs.job.service.TaskStatusService;
import app.bpartners.geojobs.model.exception.ImageSourcesTimeoutException;
import app.bpartners.geojobs.service.TaskConsumer;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;
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
      fail(task, String.format("Reached max attempt %d/%d", attemptNb, MAX_ATTEMPT_NB));
      return;
    }

    try {
      taskConsumer.accept(task);
    } catch (Exception e) {
      // /!\ catches Exception (not only RuntimeException): task consumers are @SneakyThrows, so a
      // checked exception can escape here; catching RuntimeException only would let it bypass
      // fail()
      // entirely, leaving the task stuck in UNKNOWN with no message on the aggregated job status.
      var exceptionType = e.getClass();
      if (isRetryable() && !exceptionType.equals(ImageSourcesTimeoutException.class)) {
        log.error(
            "Task [{} - id={}] failed at attempt {}/{}, it will be retried",
            task,
            task.getId(),
            attemptNb,
            MAX_ATTEMPT_NB,
            e);
        throw sneakyThrow(e); // not acked -> SQS redelivers until attemptNb reaches MAX_ATTEMPT_NB
      }
      log.error(
          "Task [{} - id={}] failed, marking it as FAILED without retry", task, task.getId(), e);
      fail(task, messageOf(e));
      return;
    }

    succeed(task);
  }

  protected void succeed(T task) {
    taskRepository.save(task);
    taskStatusService.succeed(task);
  }

  protected void fail(T task) {
    fail(task, null);
  }

  protected void fail(T task, String message) {
    taskRepository.save(task);
    taskStatusService.fail(task, message);
  }

  /**
   * Builds a non-null failure message walking the whole cause chain. {@link Throwable#getMessage()}
   * is often null (e.g. a bare NullPointerException) or hides the real detail in the cause, which
   * would then be dropped when the job status aggregates its tasks' messages. Falls back to the
   * exception simple name when a message is null. This is what surfaces on the task, then on the
   * aggregated job status.
   */
  static String messageOf(Throwable e) {
    var sb = new StringBuilder();
    Set<Throwable> seen = Collections.newSetFromMap(new IdentityHashMap<>()); // guards cause cycles
    for (var t = e; t != null && seen.add(t); t = t.getCause()) {
      if (!sb.isEmpty()) {
        sb.append(" <- ");
      }
      sb.append(t.getMessage() != null ? t.getMessage() : t.getClass().getSimpleName());
    }
    return sb.toString();
  }

  @SuppressWarnings("unchecked")
  private static <T extends Throwable> RuntimeException sneakyThrow(Throwable t) throws T {
    throw (T) t;
  }
}
