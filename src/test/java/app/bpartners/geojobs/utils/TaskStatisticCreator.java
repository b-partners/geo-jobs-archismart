package app.bpartners.geojobs.utils;

import static app.bpartners.geojobs.job.model.Status.HealthStatus.UNKNOWN;
import static app.bpartners.geojobs.job.model.Status.ProgressionStatus.PROCESSING;

import app.bpartners.geojobs.job.model.statistic.HealthStatusStatistic;
import app.bpartners.geojobs.job.model.statistic.TaskStatistic;
import app.bpartners.geojobs.job.model.statistic.TaskStatusStatistic;
import java.util.List;

public class TaskStatisticCreator {

  public TaskStatistic createProcessingTask(
      String jobId, app.bpartners.geojobs.job.model.JobType jobType) {
    return TaskStatistic.builder()
        .jobType(jobType)
        .jobId(jobId)
        .taskStatusStatistics(
            List.of(
                TaskStatusStatistic.builder()
                    .progression(PROCESSING)
                    .healthStatusStatistics(
                        List.of(HealthStatusStatistic.builder().healthStatus(UNKNOWN).build()))
                    .taskStatistic(TaskStatistic.builder().jobId(jobId).jobType(jobType).build())
                    .build()))
        .build();
  }
}
