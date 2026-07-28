package app.bpartners.geojobs.job.service;

import static app.bpartners.geojobs.job.model.Status.HealthStatus.FAILED;
import static app.bpartners.geojobs.job.model.Status.HealthStatus.SUCCEEDED;
import static app.bpartners.geojobs.job.model.Status.HealthStatus.UNKNOWN;
import static app.bpartners.geojobs.job.model.Status.ProgressionStatus.FINISHED;
import static app.bpartners.geojobs.job.model.Status.ProgressionStatus.PROCESSING;
import static java.util.Comparator.comparing;
import static java.util.Comparator.naturalOrder;
import static java.util.UUID.randomUUID;
import static java.util.stream.Collectors.toList;

import app.bpartners.geojobs.endpoint.event.EventProducer;
import app.bpartners.geojobs.endpoint.event.model.status.TaskStatisticRecomputingSubmitted;
import app.bpartners.geojobs.job.model.Job;
import app.bpartners.geojobs.job.model.JobStatus;
import app.bpartners.geojobs.job.model.Status;
import app.bpartners.geojobs.job.model.Task;
import app.bpartners.geojobs.job.model.TaskStatus;
import app.bpartners.geojobs.job.model.statistic.TaskStatistic;
import app.bpartners.geojobs.job.repository.JobStatusRepository;
import app.bpartners.geojobs.job.repository.TaskRepository;
import app.bpartners.geojobs.model.exception.NotFoundException;
import app.bpartners.geojobs.model.page.BoundedPageSize;
import app.bpartners.geojobs.model.page.PageFromOne;
import app.bpartners.geojobs.repository.TaskStatisticRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.time.Instant;
import java.util.List;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

@Slf4j
public abstract class JobService<T extends Task, J extends Job> {
  protected final JpaRepository<J, String> repository;
  protected final JobStatusRepository jobStatusRepository;
  protected final TaskStatisticRepository taskStatisticRepository;
  protected final TaskRepository<T> taskRepository;
  protected final EventProducer eventProducer;
  private final Class<J> jobClazz;

  @Setter @PersistenceContext EntityManager em;

  protected JobService(
      JpaRepository<J, String> repository,
      JobStatusRepository jobStatusRepository,
      TaskStatisticRepository taskStatisticRepository,
      TaskRepository<T> taskRepository,
      EventProducer eventProducer,
      Class<J> jobClazz) {
    this.repository = repository;
    this.jobStatusRepository = jobStatusRepository;
    this.taskStatisticRepository = taskStatisticRepository;
    this.taskRepository = taskRepository;
    this.eventProducer = eventProducer;
    this.jobClazz = jobClazz;
  }

  public TaskStatistic getTaskStatistic(J job) {
    TaskStatistic taskStatistic =
        taskStatisticRepository.findTopByJobIdOrderByUpdatedAtDesc(job.getId());
    if (taskStatistic == null) {
      return TaskStatistic.builder()
          .id(randomUUID().toString())
          .jobId(job.getId())
          .taskStatusStatistics(List.of())
          .actualJobStatus(job.getStatus())
          .jobType(job.getStatus().getJobType())
          .updatedAt(Instant.now())
          .build();
    }
    return taskStatistic.toBuilder().actualJobStatus(job.getStatus()).build();
  }

  public TaskStatistic getTaskStatistic(String jobId) {
    J job = findById(jobId);
    return getTaskStatistic(job);
  }

  public TaskStatistic computeTaskStatistics(String jobId) {
    J job = findById(jobId);
    eventProducer.accept(List.of(TaskStatisticRecomputingSubmitted.builder().jobId(jobId).build()));
    return getTaskStatistic(job);
  }

  public List<J> findAll(PageFromOne page, BoundedPageSize pageSize) {
    Pageable pageable = PageRequest.of(page.getValue() - 1, pageSize.getValue());
    return repository.findAll(pageable).toList();
  }

  public J findById(String id) {
    return repository.findById(id).orElseThrow(() -> new NotFoundException("job.id=" + id));
  }

  public J fail(String jobId, String message) {
    return fail(findById(jobId), message);
  }

  public J fail(J job, String message) {
    var oldJob = (J) job.semanticClone();
    job.hasNewStatus(
        Status.builder()
            .progression(FINISHED)
            .health(FAILED)
            .message(message)
            .creationDatetime(Instant.now())
            .build());
    onStatusChanged(oldJob, job);
    return repository.save(job);
  }

  public J recomputeStatus(J oldJob) {
    var jobId = oldJob.getId();
    var oldStatus = oldJob.getStatus();

    em.detach(oldJob); // else following getXxx will still retrieve latest version from db
    var newJob = (J) oldJob.semanticClone();

    var tasks = taskRepository.findAllByJobId(jobId);
    tasks.forEach(em::detach); // else maintaining task <--> taskStatus will result in SQL updates
    var newTaskStatuses = tasks.stream().map(Task::getStatus).collect(toList());
    newJob.hasNewStatus(jobStatusFromTaskStatuses(jobId, oldStatus, newTaskStatuses));

    var newStatus = newJob.getStatus();
    if (!oldStatus.getProgression().equals(newStatus.getProgression())
        || !oldStatus.getHealth().equals(newStatus.getHealth())) {
      onStatusChanged(oldJob, newJob);
      jobStatusRepository.save(newStatus);
    }
    return newJob;
  }

  private JobStatus jobStatusFromTaskStatuses(
      String jobId, JobStatus oldStatus, List<TaskStatus> taskStatuses) {
    var health = healthFromTaskStatuses(oldStatus, taskStatuses);
    return JobStatus.from(
        jobId,
        Status.builder()
            .progression(progressionFromTaskStatus(oldStatus, taskStatuses))
            .health(health)
            .message(messageFromTaskStatuses(taskStatuses, health))
            .creationDatetime(
                latestInstantFromTaskStatuses(taskStatuses, oldStatus.getCreationDatetime()))
            .build(),
        oldStatus.getJobType());
  }

  /**
   * Message inherited from the tasks, which hold the failure/processing details. Aggregates all the
   * distinct (non-identical) messages of the tasks sharing the job's resulting health, ordered by
   * their creation datetime and joined with "; " (e.g. every distinct FAILED task message when the
   * job is FAILED). Returns null when no such message exists.
   */
  private String messageFromTaskStatuses(
      List<TaskStatus> taskStatuses, Status.HealthStatus health) {
    var distinctMessages =
        taskStatuses.stream()
            .filter(taskStatus -> health.equals(taskStatus.getHealth()))
            .filter(taskStatus -> taskStatus.getMessage() != null)
            .sorted(comparing(Status::getCreationDatetime))
            .map(Status::getMessage)
            .distinct()
            .toList();
    return distinctMessages.isEmpty() ? null : String.join("; ", distinctMessages);
  }

  private Instant latestInstantFromTaskStatuses(
      List<TaskStatus> taskStatuses, Instant defaultInstant) {
    var sortedInstants =
        taskStatuses.stream()
            .sorted(comparing(Status::getCreationDatetime, naturalOrder()).reversed())
            .toList();
    return sortedInstants.isEmpty()
        ? defaultInstant
        : sortedInstants.getFirst().getCreationDatetime();
  }

  private Status.HealthStatus healthFromTaskStatuses(
      JobStatus oldStatus, List<TaskStatus> newTaskStatuses) {
    var newHealths = newTaskStatuses.stream().map(Status::getHealth).toList();
    return newHealths.stream().anyMatch(FAILED::equals)
        ? FAILED
        : newHealths.stream().anyMatch(UNKNOWN::equals)
            ? UNKNOWN
            : newHealths.stream().allMatch(SUCCEEDED::equals) ? SUCCEEDED : oldStatus.getHealth();
  }

  private Status.ProgressionStatus progressionFromTaskStatus(
      JobStatus oldStatus, List<TaskStatus> newTaskStatuses) {
    var newProgressions = newTaskStatuses.stream().map(Status::getProgression).toList();
    return newProgressions.stream().anyMatch(PROCESSING::equals)
        ? PROCESSING
        : newProgressions.stream().allMatch(FINISHED::equals)
            ? FINISHED
            : oldStatus.getProgression();
  }

  protected void onStatusChanged(J oldJob, J newJob) {}

  public J create(J job, List<T> tasks) {
    if (!job.isPending()) {
      throw new IllegalArgumentException(
          "Only PENDING job can be created. " + "You sure all tasks are PENDING?");
    }

    var saved = repository.save(job);
    taskRepository.saveAll(tasks);
    return saved;
  }

  protected List<T> getTasks(J job) {
    return taskRepository.findAllByJobId(job.getId());
  }
}
