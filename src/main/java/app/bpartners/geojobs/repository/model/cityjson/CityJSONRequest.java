package app.bpartners.geojobs.repository.model.cityjson;

import static app.bpartners.geojobs.endpoint.rest.model.DelimitationType.PARCEL_FREE_DELIMITATION;
import static jakarta.persistence.EnumType.STRING;
import static jakarta.persistence.FetchType.EAGER;
import static java.time.Instant.now;
import static org.hibernate.type.SqlTypes.JSON;
import static org.hibernate.type.SqlTypes.NAMED_ENUM;

import app.bpartners.geojobs.endpoint.rest.controller.mapper.FeatureMapper;
import app.bpartners.geojobs.endpoint.rest.model.DelimitationType;
import app.bpartners.geojobs.model.lidar.LidarProcessorType;
import app.bpartners.geojobs.repository.model.Feature;
import app.bpartners.geojobs.repository.model.detection.FeatureWithDelimitation;
import jakarta.persistence.*;
import java.io.Serializable;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;

@Data
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "city_json_request")
@Builder(toBuilder = true)
public class CityJSONRequest implements Serializable {
  @Id private String id;

  @Column(name = "creation_datetime")
  private Instant creationDatetime;

  @JoinColumn(referencedColumnName = "id", name = "community_owner_id")
  private String communityOwnerId;

  @Column(name = "feature_with_delimitation")
  @JdbcTypeCode(JSON)
  private List<FeatureWithDelimitation> featuresWithDelimitation;

  @Column(name = "delimitations")
  @JdbcTypeCode(JSON)
  private List<Feature> delimitations;

  @Enumerated(STRING)
  @JdbcTypeCode(NAMED_ENUM)
  private CityJSONRequestStatus status;

  @Enumerated(STRING)
  @JdbcTypeCode(NAMED_ENUM)
  private CityJSONRequestStep step;

  @OneToMany(mappedBy = "request", cascade = CascadeType.ALL, fetch = EAGER)
  private List<CityJSON> cityJsons = new ArrayList<>();

  @Enumerated(STRING)
  @JdbcTypeCode(NAMED_ENUM)
  private CityJSONDelimitationObjectType delimitationObjectType;

  @Enumerated(STRING)
  @JdbcTypeCode(NAMED_ENUM)
  @Getter(AccessLevel.NONE)
  private DelimitationType delimitationType;

  @OneToMany(mappedBy = "cityJsonRequest", cascade = CascadeType.ALL, fetch = EAGER)
  private List<CityJSONTexture> textures;

  @Column(name = "complexity_factor")
  private Float complexityFactor;

  @Column(name = "knn")
  private Integer knn;

  @PrePersist
  protected void onCreate() {
    this.creationDatetime = now().truncatedTo(ChronoUnit.MICROS);
  }

  public List<app.bpartners.geojobs.endpoint.rest.model.Feature> getRestFeatureDelimitations() {
    return delimitations == null
        ? null
        : delimitations.stream().map(FeatureMapper::toRestFeature).toList();
  }

  @Transient private LidarProcessorType lidarProcessorType;

  public List<Feature> getRequestDelimitations() {
    if (getFeaturesWithDelimitation() != null && !getFeaturesWithDelimitation().isEmpty()) {
      return getFeaturesWithDelimitation().stream()
          .map(FeatureWithDelimitation::delimitations)
          .flatMap(List::stream)
          .toList();
    }
    return getDelimitations();
  }

  public DelimitationType getDelimitationType() {
    return delimitationType == null ? PARCEL_FREE_DELIMITATION : delimitationType;
  }
}
