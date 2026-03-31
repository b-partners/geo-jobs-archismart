package app.bpartners.geojobs.utils.lidar;

import app.bpartners.geojobs.model.lidar.LasPointGeometry;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;

public class LasPointGeometryLoaderUtils {
  private static final ObjectMapper MAPPER = new ObjectMapper();

  public static Set<LasPointGeometry> readPointsFromResources(String resourcePath) {
    try (InputStream is = getResourceAsStream(resourcePath)) {

      JsonNode root = MAPPER.readTree(is);
      Set<LasPointGeometry> points = new HashSet<>();

      JsonNode features = root.get("features");
      if (features == null || !features.isArray()) {
        return points;
      }

      for (JsonNode feature : features) {
        JsonNode geometry = feature.get("geometry");
        if (geometry == null) continue;

        if (!"Point".equalsIgnoreCase(geometry.get("type").asText())) continue;

        JsonNode coordinates = geometry.get("coordinates");
        if (coordinates == null || coordinates.size() < 3) continue;

        points.add(
            new LasPointGeometry(
                coordinates.get(0).asDouble(),
                coordinates.get(1).asDouble(),
                coordinates.get(2).asDouble()));
      }

      return points;

    } catch (IOException e) {
      throw new RuntimeException("Failed to read GeoJSON from resources: " + resourcePath, e);
    }
  }

  private static InputStream getResourceAsStream(String path) {
    var is = LasPointGeometryLoaderUtils.class.getClassLoader().getResourceAsStream(path);

    if (is == null) {
      throw new IllegalArgumentException("Resource not found: " + path);
    }
    return is;
  }
}
