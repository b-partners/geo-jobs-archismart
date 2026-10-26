package app.bpartners.geojobs.service.lidar.api;

import static app.bpartners.geojobs.model.geometry.GeometryFactory.geometryFactory;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.LinearRing;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;

class GeoJsonGeometryWriterTest {
  private static Coordinate[] a_square(double x, double y, double size) {
    return new Coordinate[] {
      new Coordinate(x, y),
      new Coordinate(x + size, y),
      new Coordinate(x + size, y + size),
      new Coordinate(x, y + size),
      new Coordinate(x, y)
    };
  }

  @Test
  void writes_polygon_without_holes_as_geojson() {
    var polygon = geometryFactory.createPolygon(a_square(0, 0, 2));

    var actual = GeoJsonGeometryWriter.toGeoJson(polygon);

    assertEquals("Polygon", actual.get("type"));
    assertEquals(
        List.of(
            List.of(
                List.of(0.0, 0.0),
                List.of(2.0, 0.0),
                List.of(2.0, 2.0),
                List.of(0.0, 2.0),
                List.of(0.0, 0.0))),
        actual.get("coordinates"));
  }

  @Test
  void writes_polygon_with_hole_as_geojson() {
    LinearRing shell = geometryFactory.createLinearRing(a_square(0, 0, 10));
    LinearRing hole = geometryFactory.createLinearRing(a_square(2, 2, 2));
    Polygon polygon = geometryFactory.createPolygon(shell, new LinearRing[] {hole});

    var actual = GeoJsonGeometryWriter.toGeoJson(polygon);

    assertEquals("Polygon", actual.get("type"));
    assertEquals(
        List.of(
            List.of(
                List.of(0.0, 0.0),
                List.of(10.0, 0.0),
                List.of(10.0, 10.0),
                List.of(0.0, 10.0),
                List.of(0.0, 0.0)),
            List.of(
                List.of(2.0, 2.0),
                List.of(4.0, 2.0),
                List.of(4.0, 4.0),
                List.of(2.0, 4.0),
                List.of(2.0, 2.0))),
        actual.get("coordinates"));
  }

  @Test
  void writes_multi_polygon_as_geojson() {
    var polygon1 = geometryFactory.createPolygon(a_square(0, 0, 2));
    var polygon2 = geometryFactory.createPolygon(a_square(5, 5, 3));
    var multiPolygon = geometryFactory.createMultiPolygon(new Polygon[] {polygon1, polygon2});

    var actual = GeoJsonGeometryWriter.toGeoJson(multiPolygon);

    assertEquals("MultiPolygon", actual.get("type"));
    assertEquals(
        List.of(
            List.of(
                List.of(
                    List.of(0.0, 0.0),
                    List.of(2.0, 0.0),
                    List.of(2.0, 2.0),
                    List.of(0.0, 2.0),
                    List.of(0.0, 0.0))),
            List.of(
                List.of(
                    List.of(5.0, 5.0),
                    List.of(8.0, 5.0),
                    List.of(8.0, 8.0),
                    List.of(5.0, 8.0),
                    List.of(5.0, 5.0)))),
        actual.get("coordinates"));
  }

  @Test
  void throws_on_unsupported_geometry_type() {
    Point point = geometryFactory.createPoint(new Coordinate(0, 0));

    assertThrows(IllegalArgumentException.class, () -> GeoJsonGeometryWriter.toGeoJson(point));
  }
}
