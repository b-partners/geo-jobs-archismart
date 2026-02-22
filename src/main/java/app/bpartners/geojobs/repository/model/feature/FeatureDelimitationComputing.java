package app.bpartners.geojobs.repository.model.feature;

import static org.hibernate.type.SqlTypes.JSON;

import app.bpartners.geojobs.repository.model.detection.FeatureWithDelimitation;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
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

  @JoinColumn(referencedColumnName = "id")
  private String detectionIdentifier;

  @JdbcTypeCode(JSON)
  private FeatureWithDelimitation featureWithDelimitation;

  @Column(nullable = false, updatable = false)
  private Instant creationDatetime;
}
