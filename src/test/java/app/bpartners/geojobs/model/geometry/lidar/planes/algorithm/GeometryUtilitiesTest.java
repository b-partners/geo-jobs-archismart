package app.bpartners.geojobs.model.geometry.lidar.planes.algorithm;

import static app.bpartners.geojobs.model.geometry.GeometryFactory.geometryFactory;
import static app.bpartners.geojobs.model.lidar.planes.algorithm.GeometryUtilities.*;
import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.*;

class GeometryUtilitiesTest {
  private Polygon polygon(double[][] coords) {
    var coordinates = new Coordinate[coords.length + 1];
    for (int i = 0; i < coords.length; i++) {
      coordinates[i] = new Coordinate(coords[i][0], coords[i][1]);
    }
    coordinates[coords.length] = coordinates[0];

    return geometryFactory.createPolygon(coordinates);
  }

  @Test
  void square_should_be_compact() {
    var square =
        polygon(
            new double[][] {
              {0, 0},
              {1, 0},
              {1, 1},
              {0, 1}
            });

    assertTrue(isCompact(square, 0.1));
  }

  @Test
  void thin_rectangle_should_not_be_compact() {
    var thin =
        polygon(
            new double[][] {
              {0, 0},
              {10, 0},
              {10, 0.1},
              {0, 0.1}
            });

    assertFalse(isCompact(thin, 0.1));
  }

  @Test
  void normal_rectangle_should_be_compact() {
    var rect =
        polygon(
            new double[][] {
              {0, 0},
              {4, 0},
              {4, 2},
              {0, 2}
            });

    assertTrue(isCompact(rect, 0.1));
  }

  @Test
  void getLargestPolygon_with_single_polygon_returns_self() {
    var poly = polygon(new double[][] {{0, 0}, {10, 0}, {10, 10}, {0, 10}});

    var result = getLargestPolygon(poly);

    assertEquals(poly, result);
    assertEquals(100.0, result.getArea());
  }

  @Test
  void getLargestPolygon_with_multipolygon_returns_largest_area() {
    var small = polygon(new double[][] {{0, 0}, {2, 0}, {2, 2}, {0, 2}}); // Area 4
    var large = polygon(new double[][] {{10, 10}, {15, 10}, {15, 15}, {10, 15}}); // Area 25

    var multi = geometryFactory.createMultiPolygon(new Polygon[] {small, large});

    var result = getLargestPolygon(multi);

    assertEquals(large, result);
    assertEquals(25.0, result.getArea());
  }

  @Test
  void getLargestPolygon_throws_exception_for_point() {
    var point = geometryFactory.createPoint(new Coordinate(1, 1));

    var exception = assertThrows(IllegalArgumentException.class, () -> getLargestPolygon(point));
    assertTrue(exception.getMessage().contains("Point"));
  }

  @Test
  void extend_line() {
    var initialStart = new Coordinate(0, 0);
    var initialEnd = new Coordinate(10, 0);
    var initialLine = geometryFactory.createLineString(new Coordinate[] {initialStart, initialEnd});

    double delta = 5.0;
    var actualLine = extend(initialLine, delta);
    var actualStart = actualLine.getCoordinateN(0);
    var actualEnd = actualLine.getCoordinateN(actualLine.getNumPoints() - 1);

    assertEquals(initialLine.getLength() + delta * 2, actualLine.getLength());
    assertEquals(initialStart.getX() - delta, actualStart.getX());
    assertEquals(initialEnd.getY(), actualEnd.getY());
  }

  @Test
  void extend_oblique_line() {
    double x1 = 0;
    double y1 = 0;
    double x2 = 10;
    double y2 = 10;

    var initialStart = new Coordinate(x1, y1);
    var initialEnd = new Coordinate(x2, y2);
    var initialLine = geometryFactory.createLineString(new Coordinate[] {initialStart, initialEnd});

    double delta = 5.0;
    var actualLine = extend(initialLine, delta);
    var actualStart = actualLine.getCoordinateN(0);
    var actualEnd = actualLine.getCoordinateN(actualLine.getNumPoints() - 1);

    double offset = delta / Math.sqrt(2);
    assertEquals(initialLine.getLength() + delta * 2, actualLine.getLength(), 1e-9);
    assertEquals(x1 - offset, actualStart.getX(), 1e-9);
    assertEquals(y1 - offset, actualStart.getY(), 1e-9);
    assertEquals(x2 + offset, actualEnd.getX(), 1e-9);
    assertEquals(y2 + offset, actualEnd.getY(), 1e-9);
  }
}
