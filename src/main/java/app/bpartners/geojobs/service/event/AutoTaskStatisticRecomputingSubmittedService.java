package app.bpartners.geojobs.service.event;

import static app.bpartners.geojobs.model.exception.ApiException.ExceptionType.SERVER_EXCEPTION;

import app.bpartners.geojobs.endpoint.event.model.AutoTaskStatisticRecomputingSubmitted;
import app.bpartners.geojobs.endpoint.event.model.status.TaskStatisticRecomputingSubmitted;
import app.bpartners.geojobs.job.model.Job;
import app.bpartners.geojobs.model.exception.ApiException;
import app.bpartners.geojobs.model.exception.NotFoundException;
import app.bpartners.geojobs.repository.*;
import app.bpartners.geojobs.repository.model.detection.ParcelDetectionTask;
import app.bpartners.geojobs.repository.model.detection.ZoneDetectionJob;
import app.bpartners.geojobs.repository.model.tiling.ParcelTilingTask;
import app.bpartners.geojobs.repository.model.tiling.ZoneTilingJob;
import java.util.function.Consumer;
import org.springframework.stereotype.Service;

@Service
public class AutoTaskStatisticRecomputingSubmittedService
    implements Consumer<AutoTaskStatisticRecomputingSubmitted> {
  private static final int ATTEMPT_FOR_256_MINUTES_DURATION = 6;
  private final TaskStatisticRecomputingSubmittedConsumer<ParcelTilingTask, ZoneTilingJob>
      tilingStatisticConsumer;
  private final TaskStatisticRecomputingSubmittedConsumer<ParcelDetectionTask, ZoneDetectionJob>
      zoneDetectionStatisticConsumer;
  private final ZoneTilingJobRepository tilingJobRepository;
  private final ZoneDetectionJobRepository zoneDetectionJobRepository;

  public AutoTaskStatisticRecomputingSubmittedService(
      ZoneDetectionJobRepository zoneDetectionJobRepository,
      ParcelDetectionTaskRepository parcelDetectionTaskRepository,
      ZoneTilingJobRepository zoneTilingJobRepository,
      TilingTaskRepository tilingTaskRepository,
      TaskStatisticRepository taskStatisticRepository) {
    this.tilingJobRepository = zoneTilingJobRepository;
    this.zoneDetectionJobRepository = zoneDetectionJobRepository;
    this.zoneDetectionStatisticConsumer =
        new TaskStatisticRecomputingSubmittedConsumer<>(
            zoneDetectionJobRepository, parcelDetectionTaskRepository, taskStatisticRepository);
    this.tilingStatisticConsumer =
        new TaskStatisticRecomputingSubmittedConsumer<>(
            zoneTilingJobRepository, tilingTaskRepository, taskStatisticRepository);
  }

  @Override
  public void accept(AutoTaskStatisticRecomputingSubmitted event) {
    var jobId = event.getJobId();
    var attemptNb = event.getAttemptNb();
    if (attemptNb > ATTEMPT_FOR_256_MINUTES_DURATION) {
      return;
    }
    var optionalTiling = tilingJobRepository.findById(jobId);
    if (optionalTiling.isEmpty()) {
      var zoneDetectionJob =
          zoneDetectionJobRepository
              .findById(jobId)
              .orElseThrow(() -> new NotFoundException("Job.id=" + jobId + " not found"));
      processJob(zoneDetectionJob, jobId);
    } else {
      var tilingJob = optionalTiling.get();
      processJob(tilingJob, jobId);
    }
  }

  private void processJob(Job job, String jobId) {
    if (job.isFinished()) {
      return;
    }
    var taskStatisticRecomputingSubmitted = new TaskStatisticRecomputingSubmitted(jobId);
    if (job instanceof ZoneDetectionJob) {
      zoneDetectionStatisticConsumer.accept(taskStatisticRecomputingSubmitted);
    } else if (job instanceof ZoneTilingJob) {
      tilingStatisticConsumer.accept(taskStatisticRecomputingSubmitted);
    }
    throw new ApiException(
        SERVER_EXCEPTION, "Fail on purpose so that message is not ack, causing retry");
  }
}
