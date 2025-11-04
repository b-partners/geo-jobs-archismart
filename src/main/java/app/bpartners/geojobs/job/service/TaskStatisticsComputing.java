package app.bpartners.geojobs.job.service;

import static java.util.UUID.randomUUID;

import app.bpartners.geojobs.job.model.Status;
import app.bpartners.geojobs.job.model.Task;
import app.bpartners.geojobs.job.model.statistic.HealthStatusStatistic;
import app.bpartners.geojobs.job.model.statistic.TaskStatusStatistic;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;
import lombok.NonNull;

public class TaskStatisticsComputing<T extends Task>
    implements Function<List<T>, List<TaskStatusStatistic>> {

  @Override
  public List<TaskStatusStatistic> apply(List<T> tasks) {
    List<TaskStatusStatistic> taskStatusStatistics = new ArrayList<>();
    Stream<Status.ProgressionStatus> progressionStatuses =
        Arrays.stream(Status.ProgressionStatus.values());
    progressionStatuses.forEach(
        progressionStatus -> {
          var healthStatistics = new ArrayList<HealthStatusStatistic>();
          Arrays.stream(Status.HealthStatus.values())
              .forEach(
                  healthStatus ->
                      healthStatistics.add(
                          computeHealthStatistics(tasks, progressionStatus, healthStatus)));
          TaskStatusStatistic taskStatusStatistic =
              TaskStatusStatistic.builder()
                  .id(randomUUID().toString())
                  .progression(progressionStatus)
                  .build();
          taskStatusStatistic.addHealthStatusStatistics(healthStatistics);
          taskStatusStatistics.add(taskStatusStatistic);
        });
    return taskStatusStatistics;
  }

  @NonNull
  private HealthStatusStatistic computeHealthStatistics(
      List<T> tasks, Status.ProgressionStatus progressionStatus, Status.HealthStatus healthStatus) {
    return HealthStatusStatistic.builder()
        .id(randomUUID().toString())
        .healthStatus(healthStatus)
        .count(
            tasks.stream()
                .filter(
                    task ->
                        task.getStatus().getProgression().equals(progressionStatus)
                            && task.getStatus().getHealth().equals(healthStatus))
                .count())
        .build();
  }
}
