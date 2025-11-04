package app.bpartners.geojobs.job.model.statistic;

import static jakarta.persistence.CascadeType.ALL;
import static jakarta.persistence.EnumType.STRING;
import static jakarta.persistence.FetchType.EAGER;
import static org.hibernate.type.SqlTypes.NAMED_ENUM;

import app.bpartners.geojobs.job.model.Status;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;

@Entity(name = "\"health_status_statistic\"")
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
@Data
@ToString(exclude = "taskStatusStatistic")
public class HealthStatusStatistic {
  @Id private String id;

  @ManyToOne(cascade = ALL, fetch = EAGER)
  private TaskStatusStatistic taskStatusStatistic;

  @Enumerated(STRING)
  @JdbcTypeCode(NAMED_ENUM)
  private Status.HealthStatus healthStatus;

  private long count;
}
