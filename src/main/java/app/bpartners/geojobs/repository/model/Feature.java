package app.bpartners.geojobs.repository.model;

import static org.hibernate.type.SqlTypes.JSON;

import app.bpartners.geojobs.endpoint.rest.model.Geometry;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import java.io.Serializable;
import java.util.HashMap;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.hibernate.annotations.JdbcTypeCode;

@AllArgsConstructor
@NoArgsConstructor
@Builder(toBuilder = true)
@Data
@ToString
@EqualsAndHashCode
@JsonIgnoreProperties({"priorityLayer"})
public class Feature implements Serializable {
  private String id;
  private Integer zoom;

  @JsonDeserialize(as = HashMap.class)
  private HashMap<String, Object> properties = new HashMap<>();

  @JdbcTypeCode(JSON)
  private FeatureGeometry geometry;

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
