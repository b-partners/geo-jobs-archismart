package app.bpartners.geojobs.job.service;

import static java.util.UUID.randomUUID;

import app.bpartners.geojobs.job.model.Job;
import app.bpartners.geojobs.job.model.Task;
import app.bpartners.geojobs.job.model.statistic.TaskStatistic;
import app.bpartners.geojobs.job.repository.TaskRepository;
import app.bpartners.geojobs.repository.model.TileDetectionTask;
import app.bpartners.geojobs.repository.model.detection.ParcelDetectionTask;
import app.bpartners.geojobs.repository.model.tiling.ParcelTilingTask;
import java.time.Instant;
import java.util.function.Function;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class TaskStatisticFunction<T extends Task, J extends Job>
    implements Function<J, TaskStatistic> {
  private final TaskRepository<T> taskRepository;
  private final TaskStatisticsComputing<T> taskStatisticsComputing;

  public TaskStatisticFunction(
      TaskRepository<T> taskRepository, TaskStatisticsComputing<T> taskStatisticsComputing) {
    this.taskRepository = taskRepository;
    this.taskStatisticsComputing = taskStatisticsComputing;
  }

  @Override
  public TaskStatistic apply(J job) {
    var tasks = taskRepository.findAllByJobId(job.getId());
    var tilesCount =
        tasks.stream()
            .map(
                task -> {
                  if (task instanceof ParcelTilingTask tilingTask) {
                    return tilingTask.getParcel() == null ? 0 : tilingTask.getTiles().size();
                  } else if (task instanceof ParcelDetectionTask parcelDetectionTask) {
                    return parcelDetectionTask.getParcel() == null
                        ? 0
                        : parcelDetectionTask.getTiles().size();
                  } else if (task instanceof TileDetectionTask) {
                    return 1; // Because tileDetectionTask contains only ONE tile
                  }
                  log.error("Unknown task type for statistic computing : {}", task.getClass());
                  return 0;
                })
            .mapToInt(Integer::intValue)
            .sum();
    var taskStatusStatistics = taskStatisticsComputing.apply(tasks);
    TaskStatistic taskStatistic =
        TaskStatistic.builder()
            .id(randomUUID().toString())
            .jobId(job.getId())
            .jobType(job.getStatus().getJobType())
            .actualJobStatus(job.getStatus())
            .tilesCount(tilesCount)
            .updatedAt(Instant.now())
            .build();
    taskStatistic.addStatusStatistics(taskStatusStatistics);
    return taskStatistic;
  }
}
