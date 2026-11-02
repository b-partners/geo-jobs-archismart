package app.bpartners.geojobs.service.cityjson;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Reads the roof slope and height of a building from the CityJSON generated for it, whichever
 * generated it (3DBag roofer or threed). Only the geometry and its semantic surfaces are used, not
 * the generator-specific attributes:
 *
 * <ul>
 *   <li>slope: slope of the largest RoofSurface polygon
 *   <li>height: highest RoofSurface vertex minus the mean GroundSurface Z (or the lowest vertex
 *       when there is no GroundSurface), rounded up to the centimeter
 * </ul>
 *
 * When a city object has several LoDs (3DBag roofer), only its most detailed geometry is read.
 */
public record CityJsonRoofMetrics(double slopeInDegrees, double heightInMeters) {
  private static final ObjectMapper MAPPER = new ObjectMapper();
  private static final String ROOF_SURFACE = "RoofSurface";
  private static final String GROUND_SURFACE = "GroundSurface";

  public static Optional<CityJsonRoofMetrics> from(Collection<File> cityJsonFiles) {
    var roofPolygons = new ArrayList<List<double[]>>();
    var groundZs = new ArrayList<Double>();
    var minZ = Double.POSITIVE_INFINITY;
    for (var file : cityJsonFiles) {
      var surfaces = readSurfaces(file);
      roofPolygons.addAll(surfaces.roofPolygons());
      groundZs.addAll(surfaces.groundZs());
      minZ = Math.min(minZ, surfaces.minZ());
    }
    if (roofPolygons.isEmpty()) {
      return Optional.empty();
    }

    var largestRoofNormal =
        roofPolygons.stream()
            .map(CityJsonRoofMetrics::normal)
            .max(Comparator.comparingDouble(Vector::length))
            .orElseThrow();
    var slope = round2(largestRoofNormal.slopeInDegrees());

    var roofMaxZ =
        roofPolygons.stream()
            .flatMap(List::stream)
            .mapToDouble(vertex -> vertex[2])
            .max()
            .orElseThrow();
    var groundZ =
        groundZs.isEmpty()
            ? minZ
            : groundZs.stream().mapToDouble(Double::doubleValue).average().orElseThrow();
    var height = ceil2(roofMaxZ - groundZ);

    return Optional.of(new CityJsonRoofMetrics(slope, height));
  }

  private record Surfaces(List<List<double[]>> roofPolygons, List<Double> groundZs, double minZ) {}

  private static Surfaces readSurfaces(File cityJsonFile) {
    JsonNode cityJson;
    try {
      cityJson = MAPPER.readTree(cityJsonFile);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
    var vertices = readVertices(cityJson);

    var roofPolygons = new ArrayList<List<double[]>>();
    var groundZs = new ArrayList<Double>();
    var minZ = Double.POSITIVE_INFINITY;
    for (var cityObject : cityJson.path("CityObjects")) {
      var geometry = mostDetailedGeometry(cityObject.path("geometry"));
      if (geometry == null) continue;

      var surfaceTypes = new ArrayList<String>();
      geometry
          .path("semantics")
          .path("surfaces")
          .forEach(s -> surfaceTypes.add(s.path("type").asText()));
      var values = geometry.path("semantics").path("values");
      var boundaries = geometry.path("boundaries");
      var isSolid = "Solid".equals(geometry.path("type").asText());

      // Solid boundaries/values have one more nesting level (shells) than MultiSurface ones
      var shells = isSolid ? toList(boundaries) : List.of(boundaries);
      var shellsValues = isSolid ? toList(values) : List.of(values);
      var shellIndex = 0;
      for (var shell : shells) {
        var shellValues = shellIndex < shellsValues.size() ? shellsValues.get(shellIndex) : null;
        var polygonIndex = 0;
        for (var polygon : shell) {
          var exterior = toVertices(polygon.path(0), vertices);
          for (var vertex : exterior) minZ = Math.min(minZ, vertex[2]);

          var type = surfaceType(shellValues, polygonIndex, surfaceTypes);
          if (ROOF_SURFACE.equals(type)) {
            roofPolygons.add(exterior);
          } else if (GROUND_SURFACE.equals(type)) {
            exterior.forEach(vertex -> groundZs.add(vertex[2]));
          }
          polygonIndex++;
        }
        shellIndex++;
      }
    }
    return new Surfaces(roofPolygons, groundZs, minZ);
  }

  private static List<JsonNode> toList(JsonNode array) {
    var list = new ArrayList<JsonNode>();
    array.forEach(list::add);
    return list;
  }

  private static List<double[]> readVertices(JsonNode cityJson) {
    var transform = cityJson.path("transform");
    var vertices = new ArrayList<double[]>();
    for (var vertex : cityJson.path("vertices")) {
      var coordinates = new double[3];
      for (int axis = 0; axis < 3; axis++) {
        var value = vertex.path(axis).asDouble();
        coordinates[axis] =
            transform.isMissingNode()
                ? value
                : value * transform.path("scale").path(axis).asDouble(1)
                    + transform.path("translate").path(axis).asDouble(0);
      }
      vertices.add(coordinates);
    }
    return vertices;
  }

  private static JsonNode mostDetailedGeometry(JsonNode geometries) {
    JsonNode mostDetailed = null;
    var highestLod = Double.NEGATIVE_INFINITY;
    for (var geometry : geometries) {
      var lod = geometry.path("lod").asDouble(0);
      if (lod > highestLod) {
        highestLod = lod;
        mostDetailed = geometry;
      }
    }
    return mostDetailed;
  }

  private static String surfaceType(JsonNode shellValues, int polygonIndex, List<String> types) {
    if (shellValues == null || !shellValues.path(polygonIndex).isInt()) {
      return null;
    }
    var surfaceIndex = shellValues.path(polygonIndex).asInt();
    return surfaceIndex < types.size() ? types.get(surfaceIndex) : null;
  }

  private static List<double[]> toVertices(JsonNode ring, List<double[]> vertices) {
    var ringVertices = new ArrayList<double[]>();
    ring.forEach(index -> ringVertices.add(vertices.get(index.asInt())));
    return ringVertices;
  }

  /** Newell's method: the normal's length is twice the polygon's 3D area. */
  private static Vector normal(List<double[]> polygon) {
    double x = 0, y = 0, z = 0;
    for (int i = 0; i < polygon.size(); i++) {
      var current = polygon.get(i);
      var next = polygon.get((i + 1) % polygon.size());
      x += (current[1] - next[1]) * (current[2] + next[2]);
      y += (current[2] - next[2]) * (current[0] + next[0]);
      z += (current[0] - next[0]) * (current[1] + next[1]);
    }
    return new Vector(x, y, z);
  }

  private record Vector(double x, double y, double z) {
    double length() {
      return Math.sqrt(x * x + y * y + z * z);
    }

    double slopeInDegrees() {
      var length = length();
      return length == 0 ? 0 : Math.toDegrees(Math.acos(Math.abs(z) / length));
    }
  }

  private static double round2(double value) {
    return Math.round(value * 100) / 100.0;
  }

  private static double ceil2(double value) {
    return Math.ceil(value * 100) / 100.0;
  }
}
