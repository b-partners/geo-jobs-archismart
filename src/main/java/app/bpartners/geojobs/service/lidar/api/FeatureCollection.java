package app.bpartners.geojobs.service.lidar.api;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.*;

@Getter
@Setter
@ToString
@EqualsAndHashCode
@JsonIgnoreProperties(ignoreUnknown = true)
@Builder
public class FeatureCollection implements Serializable {
  private String type;
  private List<Feature> features;

  @AllArgsConstructor
  @Getter
  @Setter
  @ToString
  @EqualsAndHashCode
  @Builder
  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class Feature implements Serializable {
    private static final String DEFAULT_FEATURE_TYPE = "Feature";
    private Map<String, Serializable> properties;
    private Assets assets;
    private String type;

    public Feature() {}

    public Feature(Map<String, Serializable> properties, Assets assets) {
      this.assets = assets;
      this.properties = properties;
      this.type = DEFAULT_FEATURE_TYPE;
    }

    @AllArgsConstructor
    @Getter
    @Setter
    @ToString
    @EqualsAndHashCode
    @Builder
    @JsonIgnoreProperties(ignoreUnknown = true)
    @NoArgsConstructor
    public static class Assets implements Serializable {
      private Map<String, Serializable> data;

      public Optional<String> getUrl() {
        var url = data.get("href");
        return url == null ? Optional.empty() : Optional.of(url.toString());
      }
    }
  }
}
