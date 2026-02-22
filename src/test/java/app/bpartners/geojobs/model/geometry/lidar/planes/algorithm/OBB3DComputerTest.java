package app.bpartners.geojobs.model.geometry.lidar.planes.algorithm;

import static app.bpartners.geojobs.model.geometry.GeometryFactory.geometryFactory;
import static app.bpartners.geojobs.service.lidar.model.LidarClass.BATIMENT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import app.bpartners.geojobs.model.lidar.LasPointGeometry;
import app.bpartners.geojobs.model.lidar.planes.Plane3D;
import app.bpartners.geojobs.model.lidar.planes.algorithm.OBB2DComputer;
import app.bpartners.geojobs.model.lidar.planes.algorithm.OBB3DComputer;
import app.bpartners.geojobs.model.lidar.planes.algorithm.model.OBB2D;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Polygon;

class OBB3DComputerTest {
  private static final double EPS = 1e-3;
  private final OBB2DComputer obb2DComputerMock = mock();
  private final OBB3DComputer subject = new OBB3DComputer(obb2DComputerMock);

  @Test
  void should_compute_obb3d_for_polygon1() {
    var data = polygon3();

    when(obb2DComputerMock.apply(any(Plane3D.class))).thenReturn(data.getSecond());

    var plane = new Plane3D(0, 0, 1, -0, null, Set.of(), 0, 0, null); // z = 0 plane horizontal

    var actualPolygon = subject.apply(plane);

    var expected = computeExpectedCoordinates(data.getSecond(), plane);
    var actual = actualPolygon.getCoordinates();

    assertEquals(expected.length, actual.length);
    for (int i = 0; i < expected.length; i++) {
      assertEquals(expected[i].getX(), actual[i].getX(), EPS);
      assertEquals(expected[i].getY(), actual[i].getY(), EPS);
      assertEquals(expected[i].getZ(), actual[i].getZ(), EPS);
    }
  }

  private OBB2DComputerTest.Pair<Polygon, OBB2D> polygon3() {
    var coordinates =
        new Coordinate[] {
          new Coordinate(50, 300),
          new Coordinate(150, 350),
          new Coordinate(100, 450),
          new Coordinate(50, 400),
          new Coordinate(50, 300)
        };
    var polygon = geometryFactory.createPolygon(coordinates);
    var obb =
        OBB2D
            .builder()
            .center(new LasPointGeometry(75.000, 375.000, 0, BATIMENT))
            .area(12500.000)
            .angle(2.034)
            .width(111.803)
            .height(111.803)
            .build();
    return new OBB2DComputerTest.Pair<>(polygon, obb);
  }

  private Coordinate[] computeExpectedCoordinates(OBB2D obb, Plane3D plane) {
    var c = obb.center();
    double halfW = obb.width() / 2.0;
    double halfH = obb.height() / 2.0;
    double angle = obb.angle();

    double cos = Math.cos(angle);
    double sin = Math.sin(angle);

    double[][] corners2D =
        new double[][] {{-halfW, -halfH}, {halfW, -halfH}, {halfW, halfH}, {-halfW, halfH}};

    var coordinates = new Coordinate[5];
    for (int i = 0; i < 4; i++) {
      double x2d = corners2D[i][0];
      double y2d = corners2D[i][1];

      double x = c.getX() + x2d * cos - y2d * sin;
      double y = c.getY() + x2d * sin + y2d * cos;
      double z;
      double a = plane.getA();
      double b = plane.getB();
      double cc = plane.getC();
      double d = plane.getD();
      if (Math.abs(cc) > 1e-12) {
        z = (-d - a * x - b * y) / cc;
      } else {
        z = c.getZ();
      }
      coordinates[i] = new Coordinate(x, y, z);
    }
    coordinates[4] = coordinates[0];
    return coordinates;
  }
}
