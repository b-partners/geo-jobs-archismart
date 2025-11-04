package app.bpartners.geojobs.job.model;

import static jakarta.persistence.EnumType.STRING;
import static jakarta.persistence.GenerationType.IDENTITY;
import static org.hibernate.type.SqlTypes.NAMED_ENUM;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;

@MappedSuperclass
@Getter
@Setter
@SuperBuilder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties({"taskId"})
public class Status implements Serializable {
  @Id
  @GeneratedValue(strategy = IDENTITY)
  private String id;

  @Enumerated(STRING)
  @JdbcTypeCode(NAMED_ENUM)
  private ProgressionStatus progression;

  @Enumerated(STRING)
  @JdbcTypeCode(NAMED_ENUM)
  private HealthStatus health;

  @CreationTimestamp private Instant creationDatetime;
  private String message;

  public Status to(Status newStatus) {
    if (newStatus.creationDatetime.isBefore(creationDatetime)) {
      return this;
    }

    var errorMessage = String.format("Illegal status transition: old=%s, new=%s", this, newStatus);

    var oldHealth = health;
    var newHealth = newStatus.getHealth();
    var checkedNewHealth = oldHealth.to(newHealth, errorMessage);

    var oldProgression = progression;
    var newProgression = newStatus.getProgression();
    var checkedNewProgression = oldProgression.to(newProgression, newHealth, errorMessage);

    return newStatus;
  }

  @Override
  public String toString() {
    return "Status{"
        + "progression="
        + progression
        + ", health="
        + health
        + ", creationDatetime="
        + creationDatetime
        + ", message='"
        + message
        + '\''
        + '}';
  }

  public enum ProgressionStatus {
    PENDING,
    PROCESSING,
    FINISHED;

    private ProgressionStatus to(
        ProgressionStatus newProgression, HealthStatus newHealth, String errorMessage) {
      return switch (this) {
        case PENDING -> newProgression;
        case PROCESSING ->
            switch (newProgression) {
              case PENDING -> throw new IllegalArgumentException(errorMessage);
              case PROCESSING, FINISHED -> newProgression;
            };
        case FINISHED ->
            switch (newProgression) {
              case PROCESSING, PENDING ->
                  switch (newHealth) {
                    case RETRYING -> newProgression;
                    case FAILED, SUCCEEDED, UNKNOWN ->
                        throw new IllegalArgumentException(errorMessage);
                  };
              case FINISHED -> newProgression;
            };
      };
    }
  }

  public enum HealthStatus {
    UNKNOWN,
    RETRYING,
    SUCCEEDED,
    FAILED;

    private HealthStatus to(HealthStatus newHealth, String errorMessage) {
      return switch (this) {
        case UNKNOWN -> newHealth;
        case RETRYING ->
            switch (newHealth) {
              case UNKNOWN, RETRYING -> newHealth;
              case SUCCEEDED, FAILED -> throw new IllegalArgumentException(errorMessage);
            };
        case SUCCEEDED ->
            switch (newHealth) {
              case SUCCEEDED, RETRYING -> newHealth;
              case UNKNOWN, FAILED -> throw new IllegalArgumentException(errorMessage);
            };
        case FAILED ->
            switch (newHealth) {
              case FAILED, RETRYING, SUCCEEDED -> newHealth;
              case UNKNOWN -> throw new IllegalArgumentException(errorMessage);
            };
      };
    }
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    Status status = (Status) o;
    // /!\ Do NOT include id, as we want status with same (progression,health,time,message)
    // but with different id to still be equal
    return progression == status.progression
        && health == status.health
        && Objects.equals(creationDatetime, status.creationDatetime)
        && Objects.equals(message, status.message);
  }

  @Override
  public int hashCode() {
    return Objects.hash(progression, health, creationDatetime, message);
  }
}
