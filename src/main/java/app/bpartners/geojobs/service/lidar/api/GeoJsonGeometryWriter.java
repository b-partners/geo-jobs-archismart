package app.bpartners.geojobs.service.lidar.api;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.Polygon;

/** Serializes a WGS84 JTS Polygon/MultiPolygon (including holes) into a GeoJSON structure. */
public final class GeoJsonGeometryWriter {
  private GeoJsonGeometryWriter() {}

  public static Map<String, Object> toGeoJson(Geometry geometry) {
    if (geometry instanceof MultiPolygon multiPolygon) {
      var polygons =
          IntStream.range(0, multiPolygon.getNumGeometries())
              .mapToObj(i -> polygonCoordinates((Polygon) multiPolygon.getGeometryN(i)))
              .toList();
      return Map.of("type", "MultiPolygon", "coordinates", polygons);
    }
    if (geometry instanceof Polygon polygon) {
      return Map.of("type", "Polygon", "coordinates", polygonCoordinates(polygon));
    }
    throw new IllegalArgumentException(
        "Unsupported geometry type for lidar url request: " + geometry.getClass());
  }

  private static List<List<List<Double>>> polygonCoordinates(Polygon polygon) {
    var rings = new ArrayList<List<List<Double>>>();
    rings.add(ringCoordinates(polygon.getExteriorRing()));
    for (int i = 0; i < polygon.getNumInteriorRing(); i++) {
      rings.add(ringCoordinates(polygon.getInteriorRingN(i)));
    }
    return rings;
  }

  private static List<List<Double>> ringCoordinates(LineString ring) {
    return Arrays.stream(ring.getCoordinates()).map(c -> List.of(c.x, c.y)).toList();
  }
}
