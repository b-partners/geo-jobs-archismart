package app.bpartners.geojobs.model.geometry.lidar.planes.algorithm;

import static app.bpartners.geojobs.model.geometry.GeometryFactory.geometryFactory;
import static app.bpartners.geojobs.model.lidar.planes.algorithm.GeometryUtilities.isCompact;
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
}
