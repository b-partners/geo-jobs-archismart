package app.bpartners.geojobs.job.model.statistic;

import static jakarta.persistence.FetchType.EAGER;

import app.bpartners.geojobs.job.model.JobStatus;
import app.bpartners.geojobs.job.model.JobType;
import app.bpartners.geojobs.job.repository.JobTypeConverter;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import lombok.*;

@Entity(name = "\"task_statistic\"")
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
@Data
@ToString
public class TaskStatistic {
  @Id private String id;
  private String jobId;

  @Convert(converter = JobTypeConverter.class)
  private JobType jobType;

  private Instant updatedAt;

  @OneToMany(
      cascade = CascadeType.ALL,
      fetch = EAGER,
      orphanRemoval = true,
      mappedBy = "taskStatistic")
  private List<TaskStatusStatistic> taskStatusStatistics = new ArrayList<>();

  private Integer tilesCount;
  @Transient private JobStatus actualJobStatus;

  public void addStatusStatistics(List<TaskStatusStatistic> toAdd) {
    toAdd.forEach(tss -> tss.setTaskStatistic(this));
    if (this.getTaskStatusStatistics() != null) {
      this.getTaskStatusStatistics().addAll(toAdd);
    } else {
      this.setTaskStatusStatistics(toAdd);
    }
  }
}
