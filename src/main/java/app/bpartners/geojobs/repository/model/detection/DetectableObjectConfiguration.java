package app.bpartners.geojobs.repository.model.detection;

import static jakarta.persistence.EnumType.STRING;
import static org.hibernate.type.SqlTypes.NAMED_ENUM;

import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.JdbcTypeCode;

@Entity
@Getter
@SuperBuilder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class DetectableObjectConfiguration {
  @Id private String id;

  @JoinColumn(referencedColumnName = "id")
  private String detectionJobId; // TODO: rename to machineDetectionJobId

  @JoinColumn(referencedColumnName = "id")
  private String detectionId;

  @Enumerated(STRING)
  @JdbcTypeCode(NAMED_ENUM)
  private DetectableType objectType;

  private String bucketStorageName;
  private Double minConfidenceForDetection;

  public DetectableObjectConfiguration duplicate(String id, String detectionJobId) {
    return DetectableObjectConfiguration.builder()
        .id(id)
        .detectionJobId(detectionJobId)
        .objectType(this.getObjectType())
        .minConfidenceForDetection(this.minConfidenceForDetection)
        .build();
  }
}
