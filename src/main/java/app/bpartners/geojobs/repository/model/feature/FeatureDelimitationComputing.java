package app.bpartners.geojobs.repository.model.feature;

import static org.hibernate.type.SqlTypes.JSON;

import app.bpartners.geojobs.repository.model.detection.Detection;
import app.bpartners.geojobs.repository.model.detection.FeatureWithDelimitation;
import jakarta.persistence.*;
import java.time.Instant;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.JdbcTypeCode;

@Entity(name = "feature_delimitation_computing")
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder(toBuilder = true)
@Getter
@Setter
@ToString
@EqualsAndHashCode
public class FeatureDelimitationComputing {
  @Id private String id;

  private String featurePropertiesIdentifier;

  @ManyToOne
  @JoinColumn(name = "detection_identifier")
  private Detection detection;

  @JdbcTypeCode(JSON)
  private FeatureWithDelimitation featureWithDelimitation;

  @Column(nullable = false, updatable = false)
  private Instant creationDatetime;
}
