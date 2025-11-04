package app.bpartners.geojobs.service.geojson;

import app.bpartners.geojobs.endpoint.rest.model.MultiPolygon;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
@EqualsAndHashCode
public class GeoJson implements Serializable {
  private final String stringValue;
  private final List<GeoFeature> geoFeatures;

  public GeoJson(List<GeoFeature> features) {
    stringValue = geojsonString(features);
    geoFeatures = features;
  }

  public GeoJson(String featuresAsString, List<GeoFeature> features) {
    stringValue = featuresAsString;
    geoFeatures = features;
  }

  public static GeoJson fromFeatures(List<GeoFeature> features) {
    return new GeoJson(geoFeaturesAsString(features), features);
  }

  private static String geoFeaturesAsString(List<GeoFeature> geoFeatures) {
    ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
    List<Map<String, Object>> features = getGeoFeatures(geoFeatures);
    try {
      return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(features);
    } catch (JsonProcessingException e) {
      throw new RuntimeException(e);
    }
  }

  private static List<Map<String, Object>> getGeoFeatures(List<GeoFeature> geoFeatures) {
    List<Map<String, Object>> features = new ArrayList<>();

    for (var geoF : geoFeatures) {
      Map<String, Object> featureAsMap = new HashMap<>();
      var geometry = geoF.getGeometry();
      var coordinates = geometry.getCoordinates();

      Map<String, Object> geometryAsMap = new HashMap<>();
      featureAsMap.put("type", "Feature");
      geometryAsMap.put("type", geometry.getType());
      geometryAsMap.put("coordinates", coordinates);

      featureAsMap.put("geometry", geometryAsMap);
      featureAsMap.put("properties", geoF.getProperties());

      features.add(featureAsMap);
    }
    return features;
  }

  private static String geojsonString(List<GeoFeature> geoFeatures) {
    ObjectMapper objectMapper = new ObjectMapper();

    Map<String, Object> geoJson = new HashMap<>();
    geoJson.put("type", "FeatureCollection");

    geoJson.put("features", getGeoFeatures(geoFeatures));

    try {
      return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(geoJson);
    } catch (JsonProcessingException e) {
      throw new RuntimeException(e);
    }
  }

  @AllArgsConstructor
  @Getter
  @Setter
  @ToString
  @EqualsAndHashCode
  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class GeoFeature implements Serializable {
    private static final String DEFAULT_FEATURE_TYPE = "Feature";
    private Map<String, Object> properties;
    private String type;
    private MultiPolygon geometry;

    public GeoFeature() {}

    public GeoFeature(Map<String, Object> properties, MultiPolygon geometry) {
      this.properties = properties;
      this.type = DEFAULT_FEATURE_TYPE;
      this.geometry = geometry;
    }
  }
}
