package app.bpartners.geojobs.endpoint.rest.controller.mapper;

import app.bpartners.geojobs.endpoint.rest.model.*;
import app.bpartners.geojobs.job.model.JobStatus;
import app.bpartners.geojobs.model.exception.NotImplementedException;
import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class TaskStatisticMapper {
  private final StatusMapper<JobStatus> statusMapper;

  public TaskStatistic toRest(app.bpartners.geojobs.job.model.statistic.TaskStatistic domain) {
    return new TaskStatistic()
        .jobId(domain.getJobId())
        .jobType(getJobType(domain.getJobType()))
        .taskStatusStatistics(
            domain.getTaskStatusStatistics().stream().map(TaskStatisticMapper::toRest).toList())
        .actualJobStatus(statusMapper.toRest(domain.getActualJobStatus()))
        .updatedAt(domain.getUpdatedAt());
  }

  private static TaskStatusStatistic toRest(
      app.bpartners.geojobs.job.model.statistic.TaskStatusStatistic taskStatusStatistic) {
    return new TaskStatusStatistic()
        .progression(getProgressionStatus(taskStatusStatistic.getProgression()))
        .healthStatistics(
            taskStatusStatistic.getHealthStatusStatistics().stream()
                .map(TaskStatisticMapper::toRest)
                .toList());
  }

  private static HealthStatusStatistic toRest(
      app.bpartners.geojobs.job.model.statistic.HealthStatusStatistic healthStatusStatistic) {
    return new HealthStatusStatistic()
        .health(getHealthStatus(healthStatusStatistic.getHealthStatus()))
        .count(BigDecimal.valueOf(healthStatusStatistic.getCount()));
  }

  private static HealthStatus getHealthStatus(
      app.bpartners.geojobs.job.model.Status.HealthStatus healthStatus) {
    switch (healthStatus) {
      case UNKNOWN -> {
        return HealthStatus.UNKNOWN;
      }
      case RETRYING -> {
        return HealthStatus.RETRYING;
      }
      case FAILED -> {
        return HealthStatus.FAILED;
      }
      case SUCCEEDED -> {
        return HealthStatus.SUCCEEDED;
      }
      default -> throw new NotImplementedException("Unknown healthStatus " + healthStatus);
    }
  }

  private static ProgressionStatus getProgressionStatus(
      app.bpartners.geojobs.job.model.Status.ProgressionStatus progressionStatus) {
    switch (progressionStatus) {
      case PENDING -> {
        return ProgressionStatus.PENDING;
      }
      case PROCESSING -> {
        return ProgressionStatus.PROCESSING;
      }
      case FINISHED -> {
        return ProgressionStatus.FINISHED;
      }
      default ->
          throw new NotImplementedException("Unknown progression status " + progressionStatus);
    }
  }

  private static JobType getJobType(app.bpartners.geojobs.job.model.JobType jobType) {
    String jobTypeName = jobType.name();
    return switch (jobTypeName) {
      case "DETECTION" -> JobType.DETECTION;
      case "TILING" -> JobType.TILING;
      default -> throw new NotImplementedException("Unknown jobType " + jobTypeName);
    };
  }
}
