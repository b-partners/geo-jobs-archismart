package app.bpartners.geojobs.repository.model.detection;

import static jakarta.persistence.EnumType.STRING;
import static java.time.Instant.now;
import static org.hibernate.type.SqlTypes.JSON;
import static org.hibernate.type.SqlTypes.NAMED_ENUM;

import app.bpartners.geojobs.repository.model.Feature;
import jakarta.persistence.*;
import java.time.Instant;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;

@Entity(name = "detection_feature")
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Data
public class DetectionFeature {
  @Id private String id;

  @Column(name = "id_feature")
  private String idFeature;

  @ManyToOne
  @JoinColumn(name = "id_detection")
  private Detection detection;

  @Enumerated(STRING)
  @JdbcTypeCode(NAMED_ENUM)
  private DetectionFeatureType detectionFeatureType;

  @JdbcTypeCode(JSON)
  private Feature feature;

  private Instant creationDatetime;

  @PrePersist
  public void onCreation() {
    this.creationDatetime = now();
  }
}
