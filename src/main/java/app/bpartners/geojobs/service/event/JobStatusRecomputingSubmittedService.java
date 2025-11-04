package app.bpartners.geojobs.service.event;

import app.bpartners.geojobs.endpoint.event.model.status.JobStatusRecomputingSubmitted;
import app.bpartners.geojobs.job.model.Job;
import app.bpartners.geojobs.job.model.Task;
import app.bpartners.geojobs.job.repository.TaskRepository;
import app.bpartners.geojobs.job.service.JobService;
import app.bpartners.geojobs.job.service.TaskStatusService;
import java.util.function.Consumer;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

@AllArgsConstructor
@Slf4j
public class JobStatusRecomputingSubmittedService<
        J extends Job, T extends Task, E extends JobStatusRecomputingSubmitted>
    implements Consumer<E> {

  private static final int MAX_ATTEMPTS = 8;
  private final JobService<T, J> jobService;
  private final TaskStatusService<T> taskStatusService;
  private final TaskRepository<T> taskRepository;

  @Override
  @SneakyThrows
  public void accept(E event) {
    log.info("Accepting event={}", event);
    var attemptNb = event.getAttemptNb();
    var jobId = event.getJobId();
    var oldJob = jobService.findById(jobId);
    log.info(
        "oldJob.id={}, oldJob.status={}, oldJob.statusHistory={}",
        oldJob.getId(),
        oldJob.getStatus(),
        oldJob.getStatusHistory());
    if (oldJob.isFinished()) {
      log.info("Job.id={} finished, oldJob.status={}", jobId, oldJob.getStatus());
      return;
    }

    if (attemptNb > MAX_ATTEMPTS) {
      fail(oldJob);
      return;
    }

    var newJob = jobService.recomputeStatus(oldJob);
    log.info(
        "job.id={}, oldJob.status={}, oldJob.statusHistory={}, newJob.status={},"
            + " newJob.statusHistory={}",
        oldJob.getId(),
        oldJob.getStatus(),
        oldJob.getStatusHistory(),
        newJob.getStatus(),
        newJob.getStatusHistory());
    throw new RuntimeException("Fail on purpose so that message is not ack, causing retry");
  }

  private void fail(J oldJob) {
    var tasks = taskRepository.findAllByJobId(oldJob.getId());
    var notFinishedTasks = tasks.stream().filter(task -> !task.isFinished()).toList();
    notFinishedTasks.forEach(taskStatusService::fail);
    jobService.recomputeStatus(oldJob);
  }
}
