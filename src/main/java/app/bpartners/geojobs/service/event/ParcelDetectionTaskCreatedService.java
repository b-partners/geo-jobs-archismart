package app.bpartners.geojobs.service.event;

import app.bpartners.geojobs.endpoint.event.model.parcel.ParcelDetectionTaskCreated;
import app.bpartners.geojobs.job.repository.TaskRepository;
import app.bpartners.geojobs.job.service.TaskStatusService;
import app.bpartners.geojobs.repository.model.detection.ParcelDetectionTask;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class ParcelDetectionTaskCreatedService
    extends TaskCreatedService<ParcelDetectionTask, ParcelDetectionTaskCreated> {
  private final ParcelDetectionTaskConsumer parcelDetectionTaskConsumer;

  public ParcelDetectionTaskCreatedService(
      ParcelDetectionTaskConsumer taskConsumer,
      TaskStatusService<ParcelDetectionTask> taskStatusService,
      TaskRepository<ParcelDetectionTask> taskRepository) {
    super(taskConsumer, taskStatusService, taskRepository);
    this.parcelDetectionTaskConsumer = taskConsumer;
  }

  @Override
  public void accept(ParcelDetectionTaskCreated parcelDetectionTaskCreated) {
    var task = parcelDetectionTaskCreated.getTask();
    var attemptNb = parcelDetectionTaskCreated.getAttemptNb();

    if (attemptNb == 1) {
      taskStatusService.process(task);
    }

    try {
      parcelDetectionTaskConsumer.accept(task);
    } catch (RuntimeException e) {
      log.error(
          "ParcelDetectionTask [id={}] failed, marking it as FAILED without retry",
          task.getId(),
          e);
      fail(task);
    }
  }
}
