package app.bpartners.geojobs.endpoint.rest.controller.mapper;

import app.bpartners.geojobs.endpoint.rest.model.DetectionStep;
import app.bpartners.geojobs.endpoint.rest.model.DetectionStepName;
import app.bpartners.geojobs.endpoint.rest.model.DetectionStepStatistic;
import app.bpartners.geojobs.endpoint.rest.model.HealthStatus;
import app.bpartners.geojobs.endpoint.rest.model.HealthStatusStatistic;
import app.bpartners.geojobs.endpoint.rest.model.ProgressionStatus;
import app.bpartners.geojobs.job.model.JobStatus;
import app.bpartners.geojobs.model.exception.NotImplementedException;
import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class DetectionStepStatisticMapper {
  private final StatusMapper<JobStatus> statusMapper;

  public DetectionStep toRestDetectionStepStatus(
      app.bpartners.geojobs.job.model.statistic.TaskStatistic statistic,
      DetectionStepName detectionStepName) {
    return new DetectionStep()
        .name(detectionStepName)
        .status(statusMapper.toRest(statistic.getActualJobStatus()))
        .updatedAt(statistic.getUpdatedAt())
        .statistics(toRestDetectionStep(statistic));
  }

  @NonNull
  private List<DetectionStepStatistic> toRestDetectionStep(
      app.bpartners.geojobs.job.model.statistic.TaskStatistic statistic) {
    return statistic.getTaskStatusStatistics().stream()
        .map(
            statusStatistic ->
                new DetectionStepStatistic()
                    .progression(getProgressionStatus(statusStatistic.getProgression()))
                    .healthStatistics(
                        statusStatistic.getHealthStatusStatistics().stream()
                            .map(DetectionStepStatisticMapper::toRest)
                            .collect(Collectors.toList())))
        .collect(Collectors.toList());
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
}
