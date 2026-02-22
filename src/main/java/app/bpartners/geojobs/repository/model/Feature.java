package app.bpartners.geojobs.repository.model;

import static org.hibernate.type.SqlTypes.JSON;

import app.bpartners.geojobs.endpoint.rest.model.Geometry;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;

@AllArgsConstructor
@NoArgsConstructor
@Builder(toBuilder = true)
@Data
@ToString
@JsonIgnoreProperties({"priorityLayer"})
public class Feature implements Serializable {
  private String id;
  private Integer zoom;

  @JsonDeserialize(as = HashMap.class)
  @Setter(AccessLevel.NONE)
  private HashMap<String, Object> properties = new HashMap<>();

  public void setProperties(Map<String, Object> properties) {
    this.properties = properties == null ? null : new HashMap<>(properties);
  }

  @JdbcTypeCode(JSON)
  private FeatureGeometry geometry;

  @Override
  public boolean equals(Object o) {
    if (!(o instanceof Feature feature)) return false;
    return Objects.equals(id, feature.id)
        && Objects.equals(zoom, feature.zoom)
        && String.valueOf(properties)
            .equals(String.valueOf(feature.properties)) // TODO: seems weird and bad
        && Objects.equals(geometry, feature.geometry);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, zoom, properties, geometry);
  }

  boolean deepEquals(Object o1, Object o2) {
    if (o1 instanceof Map && o2 instanceof Map) {
      return ((Map<?, ?>) o1).equals(o2);
    }
    if (o1 instanceof List && o2 instanceof List) {
      return ((List<?>) o1).equals(o2);
    }
    return Objects.equals(o1, o2);
  }

  @NoArgsConstructor
  @AllArgsConstructor
  @Builder(toBuilder = true)
  @Data
  @ToString
  @EqualsAndHashCode
  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class FeatureGeometry implements Serializable {
    @JsonProperty("geometryType")
    private Geometry.TypeEnum geometryType;

    @JsonProperty("actualInstanceStringValue")
    private String actualInstanceStringValue;
  }
}
