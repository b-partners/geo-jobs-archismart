package app.bpartners.geojobs.service.lidar.model.geometry;

import static app.bpartners.geojobs.model.geometry.GeometryFactory.geometryFactory;
import static org.junit.jupiter.api.Assertions.assertEquals;

import app.bpartners.geojobs.model.lidar.Polygon3DArea;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;

class Polygon3DAreaTest {
  @Test
  void test_on_polygon_without_slope() {
    var coordinates =
        new Coordinate[] {
          new Coordinate(0, 0, 0),
          new Coordinate(2, 0, 0),
          new Coordinate(2, 3, 0),
          new Coordinate(0, 3, 0),
          new Coordinate(0, 0, 0),
        };

    var polygon = geometryFactory.createPolygon(coordinates);

    var expected = 6;
    var actual = new Polygon3DArea(polygon).getValue();

    assertEquals(expected, actual);
  }

  @Test
  void test_on_polygon_with_slope() {
    var coordinates =
        new Coordinate[] {
          new Coordinate(0, 0, 0),
          new Coordinate(2, 0, 1),
          new Coordinate(2, 3, 1),
          new Coordinate(0, 3, 0),
          new Coordinate(0, 0, 0),
        };

    var polygon = geometryFactory.createPolygon(coordinates);

    var expected = 6.71;
    var actual = new Polygon3DArea(polygon).getValue();

    assertEquals(expected, actual, 0.01);
  }
}
