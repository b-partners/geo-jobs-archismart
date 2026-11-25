package app.bpartners.geojobs.service.google.geocoding;

import static org.junit.jupiter.api.Assertions.assertEquals;

import app.bpartners.geojobs.service.google.geocoding.api.GeoJsonFeature;
import app.bpartners.geojobs.service.google.geocoding.api.GoogleGeocodingClientApi;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LinearRing;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Disabled("Local use only: calls the paid Google Geocoding API")
class GeometryAddressCsvExportIT {
  private static final String CSV_HEADER = "id,creation_datetime,address,geometry";

  private final ObjectMapper om = new ObjectMapper();
  private final GeometryFactory geometryFactory = new GeometryFactory();
  private final GoogleGeocodingClientApi googleClient =
      new GoogleGeocodingClientApi(new RestTemplate(), System.getenv("GOOGLE_API_KEY"));

  @SneakyThrows
  @Test
  void export_geometry_addresses_to_csv() {
    var inputPath = Path.of(requiredEnv("GEOMETRIES_FILE"));
    var outputPath =
        Path.of(
            Optional.ofNullable(System.getenv("OUTPUT_CSV"))
                .orElse("build/geometry_addresses_" + System.currentTimeMillis() + ".csv"));
    var entries = om.readTree(inputPath.toFile());
    List<String> lines = new ArrayList<>();
    lines.add(CSV_HEADER);

    for (JsonNode entry : entries) {
      var id = entry.path("id").asText();
      var creationDatetime = entry.path("creation_datetime").asText();
      var geometry = extractGeometry(entry);
      var address = resolveAddress(id, geometry);
      lines.add(
          String.join(
              ",",
              escapeCsv(id),
              escapeCsv(creationDatetime),
              escapeCsv(address),
              escapeCsv(geometry)));
    }

    if (outputPath.getParent() != null) {
      Files.createDirectories(outputPath.getParent());
    }
    Files.write(outputPath, lines);
    log.info("CSV written to {}", outputPath.toAbsolutePath());
    assertEquals(entries.size() + 1, lines.size());
  }

  private String extractGeometry(JsonNode entry) {
    var delimitations = entry.path("delimitations");
    if (delimitations.isEmpty()) {
      return "";
    }
    return delimitations.get(0).path("geometry").path("actualInstanceStringValue").asText();
  }

  private String resolveAddress(String id, String geometry) {
    try {
      if (geometry.isEmpty()) {
        return "";
      }
      var point = interiorPoint(om.readTree(geometry));
      return googleClient
          .findBuildingByLocation(point.getY(), point.getX())
          .map(GeoJsonFeature::properties)
          .map(properties -> properties.get("formattedAddress"))
          .map(Object::toString)
          .orElse("");
    } catch (Exception e) {
      log.error("Address resolution failed for {}", id, e);
      return "";
    }
  }

  private Point interiorPoint(JsonNode polygonGeometry) {
    var rings = polygonGeometry.path("coordinates");
    var shell = toLinearRing(rings.get(0));
    var holes = new LinearRing[rings.size() - 1];
    for (int i = 1; i < rings.size(); i++) {
      holes[i - 1] = toLinearRing(rings.get(i));
    }
    Polygon polygon = geometryFactory.createPolygon(shell, holes);
    return polygon.getInteriorPoint();
  }

  private LinearRing toLinearRing(JsonNode ring) {
    var coordinates = new Coordinate[ring.size()];
    for (int i = 0; i < ring.size(); i++) {
      coordinates[i] = new Coordinate(ring.get(i).get(0).asDouble(), ring.get(i).get(1).asDouble());
    }
    return geometryFactory.createLinearRing(coordinates);
  }

  private static String requiredEnv(String name) {
    return Optional.ofNullable(System.getenv(name))
        .filter(value -> !value.isBlank())
        .orElseThrow(() -> new IllegalStateException("Missing env var " + name));
  }

  private String escapeCsv(String value) {
    if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
      return "\"" + value.replace("\"", "\"\"") + "\"";
    }
    return value;
  }
}
