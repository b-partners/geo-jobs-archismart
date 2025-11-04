package app.bpartners.geojobs.service;

import app.bpartners.geojobs.endpoint.event.EventProducer;
import app.bpartners.geojobs.endpoint.event.model.annotation.AnnotationDeliveryJobCreated;
import app.bpartners.geojobs.endpoint.event.model.annotation.AnnotationDeliveryJobStatusChanged;
import app.bpartners.geojobs.endpoint.event.model.annotation.AnnotationDeliveryJobStatusRecomputingSubmitted;
import app.bpartners.geojobs.endpoint.event.model.annotation.AnnotationDeliveryTaskCreated;
import app.bpartners.geojobs.job.repository.JobStatusRepository;
import app.bpartners.geojobs.job.repository.TaskRepository;
import app.bpartners.geojobs.job.service.JobService;
import app.bpartners.geojobs.repository.TaskStatisticRepository;
import app.bpartners.geojobs.repository.model.annotation.AnnotationDeliveryJob;
import app.bpartners.geojobs.repository.model.annotation.AnnotationDeliveryTask;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
public class AnnotationDeliveryJobService
    extends JobService<AnnotationDeliveryTask, AnnotationDeliveryJob> {
  private final EventProducer eventProducer;

  public AnnotationDeliveryJobService(
      JpaRepository<AnnotationDeliveryJob, String> repository,
      JobStatusRepository jobStatusRepository,
      TaskStatisticRepository taskStatisticRepository,
      TaskRepository<AnnotationDeliveryTask> taskRepository,
      EventProducer eventProducer) {
    super(
        repository,
        jobStatusRepository,
        taskStatisticRepository,
        taskRepository,
        eventProducer,
        AnnotationDeliveryJob.class);
    this.eventProducer = eventProducer;
  }

  @Transactional
  public void fireTasks(AnnotationDeliveryJob job) {
    List<AnnotationDeliveryTask> tasks = getTasks(job);
    log.info("DEBUG processing AnnotationDeliveryTasks size ={}", tasks.size());
    if (tasks.isEmpty()) {
      throw new IllegalStateException(
          "Unable to fire empty tasks for AnnotationDeliveryJob(id=" + job.getId() + ")");
    }
    tasks.forEach(
        task ->
            eventProducer.accept(
                List.of(AnnotationDeliveryTaskCreated.builder().deliveryTask(task).build())));
  }

  @Override
  @Transactional
  public AnnotationDeliveryJob create(
      AnnotationDeliveryJob job, List<AnnotationDeliveryTask> tasks) {
    var newJob = super.create(job, tasks);
    log.info("DEBUG AnnotationDeliveryJob.old={}, new={}", job, newJob);
    log.info("DEBUG AnnotationDeliveryTasks size ={}", getTasks(newJob).size());
    eventProducer.accept(List.of(new AnnotationDeliveryJobCreated(newJob)));
    eventProducer.accept(
        List.of(new AnnotationDeliveryJobStatusRecomputingSubmitted(newJob.getId())));
    return newJob;
  }

  @Override
  protected void onStatusChanged(AnnotationDeliveryJob oldJob, AnnotationDeliveryJob newJob) {
    eventProducer.accept(
        List.of(
            AnnotationDeliveryJobStatusChanged.builder().oldJob(oldJob).newJob(newJob).build()));
  }
}
