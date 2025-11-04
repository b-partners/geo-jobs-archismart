package app.bpartners.geojobs.repository.model.detection;

import static app.bpartners.geojobs.endpoint.rest.controller.mapper.FeatureMapper.toRestFeature;
import static jakarta.persistence.CascadeType.ALL;
import static jakarta.persistence.EnumType.STRING;
import static jakarta.persistence.FetchType.EAGER;
import static org.hibernate.type.SqlTypes.JSON;
import static org.hibernate.type.SqlTypes.NAMED_ENUM;

import app.bpartners.geojobs.repository.model.Feature;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.io.Serializable;
import java.util.List;
import java.util.Optional;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;

@Entity
@Table(name = "\"detected_object\"")
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Getter
@Setter
@EqualsAndHashCode
@ToString
public class DetectedObject implements Serializable {
  @Id private String id;

  @JdbcTypeCode(JSON)
  @Getter(AccessLevel.NONE)
  private Feature feature;

  @JoinColumn(referencedColumnName = "id")
  private String detectedTileId;

  @OneToOne(cascade = ALL, fetch = EAGER)
  private DetectableObjectType detectedObjectType;

  private Double computedConfidence;

  @Enumerated(STRING)
  @JdbcTypeCode(NAMED_ENUM)
  private ZoneDetectionJob.DetectionType type;

  public app.bpartners.geojobs.endpoint.rest.model.Feature getFeature() {
    return toRestFeature(feature);
  }

  public boolean isInDoubt(List<DetectableObjectConfiguration> objectConfigurations) {
    DetectableType detectableObjectType = getDetectableObjectType();
    Optional<DetectableObjectConfiguration> optionalConfiguration =
        objectConfigurations.stream()
            .filter(
                detectableObjectConfiguration ->
                    detectableObjectConfiguration.getObjectType().equals(detectableObjectType))
            .findFirst();
    return optionalConfiguration.isPresent()
        && optionalConfiguration.get().getMinConfidenceForDetection() != null
        && computedConfidence != null
        && optionalConfiguration.get().getMinConfidenceForDetection() >= computedConfidence;
  }

  public DetectableType getDetectableObjectType() {
    return detectedObjectType.getDetectableType();
  }
}
