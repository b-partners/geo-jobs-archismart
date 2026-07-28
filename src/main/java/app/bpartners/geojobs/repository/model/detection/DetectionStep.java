package app.bpartners.geojobs.repository.model.detection;

import static jakarta.persistence.EnumType.STRING;
import static org.hibernate.type.SqlTypes.NAMED_ENUM;

import app.bpartners.geojobs.endpoint.rest.model.DetectionStepName;
import app.bpartners.geojobs.job.model.statistic.TaskStatistic;
import jakarta.persistence.*;
import java.time.Instant;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;

@Entity
@Getter
@Setter
@Builder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "detection_step")
@EqualsAndHashCode
@ToString
public class DetectionStep {
  @Id private String id;

  @Enumerated(STRING)
  @JdbcTypeCode(NAMED_ENUM)
  private DetectionStepName name;

  @Enumerated(STRING)
  @JdbcTypeCode(NAMED_ENUM)
  private app.bpartners.geojobs.job.model.Status.ProgressionStatus progression;

  @Enumerated(STRING)
  @JdbcTypeCode(NAMED_ENUM)
  private app.bpartners.geojobs.job.model.Status.HealthStatus health;

  @Column(name = "creation_datetime")
  private Instant creationDatetime;

  @Column(name = "detection_id")
  private String detectionId;

  private String message;

  /**
   * Optional detailed task statistics backing this step when it is a request-time computed step
   * (tiling / machine detection). Never persisted.
   */
  @Transient @EqualsAndHashCode.Exclude @ToString.Exclude private TaskStatistic statistic;
}
