package app.bpartners.geojobs.job.model.statistic;

import static jakarta.persistence.CascadeType.ALL;
import static jakarta.persistence.EnumType.STRING;
import static jakarta.persistence.FetchType.EAGER;
import static org.hibernate.type.SqlTypes.NAMED_ENUM;

import app.bpartners.geojobs.job.model.Status;
import jakarta.persistence.*;
import java.util.List;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;

@Entity(name = "\"task_status_statistic\"")
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
@Data
@ToString(exclude = "taskStatistic")
public class TaskStatusStatistic {
  @Id private String id;

  @ManyToOne(fetch = EAGER, cascade = ALL)
  private TaskStatistic taskStatistic;

  @Enumerated(STRING)
  @JdbcTypeCode(NAMED_ENUM)
  private Status.ProgressionStatus progression;

  @OneToMany(cascade = ALL, fetch = EAGER, orphanRemoval = true, mappedBy = "taskStatusStatistic")
  private List<HealthStatusStatistic> healthStatusStatistics;

  public void addHealthStatusStatistics(List<HealthStatusStatistic> toAdd) {
    toAdd.forEach(hss -> hss.setTaskStatusStatistic(this));
    if (this.getHealthStatusStatistics() != null) {
      this.getHealthStatusStatistics().addAll(toAdd);
    } else {
      this.setHealthStatusStatistics(toAdd);
    }
  }
}
