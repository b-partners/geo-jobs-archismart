package app.bpartners.geojobs.service.lidar.api;

import static app.bpartners.geojobs.model.exception.ApiException.ExceptionType.SERVER_EXCEPTION;

import app.bpartners.geojobs.model.exception.ApiException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.geom.PrecisionModel;
import org.locationtech.jts.geom.util.GeometryFixer;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

@Component
public class SwissBoundaryChecker {
  private final Geometry swissBoundary;

  public SwissBoundaryChecker() {
    try {
      ClassPathResource resource = new ClassPathResource("files/swiss_boundary.geojson");
      String geoJson = new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
      this.swissBoundary = parseGeoJsonPolygon(geoJson);
    } catch (Exception e) {
      throw new ApiException(SERVER_EXCEPTION, e);
    }
  }

  public boolean isGeometryInSwiss(Geometry geometry) {
    Geometry fixed = GeometryFixer.fix(geometry);
    return swissBoundary.getEnvelopeInternal().intersects(fixed.getEnvelopeInternal())
        && swissBoundary.contains(fixed);
  }

  private Geometry parseGeoJsonPolygon(String geoJson) throws JsonProcessingException {
    ObjectMapper mapper = new ObjectMapper();
    JsonNode root = mapper.readTree(geoJson);
    String coordinates = "coordinates";
    String type = root.get("type").asText();
    GeometryFactory factory = new GeometryFactory(new PrecisionModel(), 4326);

    return switch (type) {
      case "Polygon" -> parsePolygon(root.get(coordinates), factory);
      case "MultiPolygon" -> {
        JsonNode polygonsNode = root.get(coordinates);
        Polygon[] polygons = new Polygon[polygonsNode.size()];
        for (int i = 0; i < polygonsNode.size(); i++) {
          polygons[i] = parsePolygon(polygonsNode.get(i), factory);
        }
        yield factory.createMultiPolygon(polygons);
      }
      case "FeatureCollection" -> {
        JsonNode features = root.get("features");
        Polygon[] polygons = new Polygon[features.size()];
        for (int i = 0; i < features.size(); i++) {
          JsonNode geom = features.get(i).get("geometry");
          polygons[i] = parsePolygon(geom.get(coordinates), factory);
        }
        yield factory.createMultiPolygon(polygons);
      }
      default -> throw new IllegalArgumentException("Unsupported GeoJSON type: " + type);
    };
  }

  private Polygon parsePolygon(JsonNode coordinatesNode, GeometryFactory factory) {
    JsonNode outerRing = coordinatesNode.get(0);
    Coordinate[] coords = new Coordinate[outerRing.size()];
    for (int i = 0; i < outerRing.size(); i++) {
      JsonNode point = outerRing.get(i);
      coords[i] = new Coordinate(point.get(0).asDouble(), point.get(1).asDouble());
    }
    return factory.createPolygon(coords);
  }
}
