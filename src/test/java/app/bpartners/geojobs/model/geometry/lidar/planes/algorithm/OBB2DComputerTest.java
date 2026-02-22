package app.bpartners.geojobs.model.geometry.lidar.planes.algorithm;

import static app.bpartners.geojobs.model.geometry.GeometryFactory.geometryFactory;
import static app.bpartners.geojobs.service.lidar.model.LidarClass.BATIMENT;

import app.bpartners.geojobs.model.lidar.LasPointGeometry;
import app.bpartners.geojobs.model.lidar.planes.algorithm.OBB2DComputer;
import app.bpartners.geojobs.model.lidar.planes.algorithm.model.OBB2D;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.*;

class OBB2DComputerTest {
  private final OBB2DComputer subject = new OBB2DComputer();
  private static final double EPS = 1e-3;

  @Test
  void test_all_polygons() {
    for (int i = 1; i <= 6; i++) {
      var data =
          switch (i) {
            case 1 -> polygon1();
            case 2 -> polygon2();
            case 3 -> polygon3();
            case 4 -> polygon4();
            case 5 -> polygon5();
            case 6 -> polygon6();
            default -> throw new IllegalStateException();
          };

      var actual = subject.apply(data.getFirst());
      var expected = data.getSecond();
      assertEquals(expected, actual);
    }
  }

  @Getter
  @RequiredArgsConstructor
  static class Pair<T, R> {
    private final T first;
    private final R second;
  }

  private Pair<Polygon, OBB2D> polygon1() {
    var coordinates =
        new Coordinate[] {
          new Coordinate(50, 50),
          new Coordinate(200, 60),
          new Coordinate(180, 200),
          new Coordinate(60, 180),
          new Coordinate(50, 50)
        };
    var polygon = geometryFactory.createPolygon(coordinates);
    var obb =
        OBB2D
            .builder()
            .center(new LasPointGeometry(120.310, 125.354, 0, BATIMENT))
            .area(21199.999)
            .angle(0.067)
            .width(150.333)
            .height(141.020)
            .build();
    return new Pair<>(polygon, obb);
  }

  private Pair<Polygon, OBB2D> polygon2() {
    var coordinates =
        new Coordinate[] {
          new Coordinate(300, 50),
          new Coordinate(450, 50),
          new Coordinate(450, 200),
          new Coordinate(300, 200),
          new Coordinate(300, 50)
        };
    var polygon = geometryFactory.createPolygon(coordinates);
    var obb =
        OBB2D
            .builder()
            .center(new LasPointGeometry(375.000, 125.000, 0, BATIMENT))
            .area(22500.000)
            .angle(-1.571)
            .width(150.000)
            .height(150.000)
            .build();
    return new Pair<>(polygon, obb);
  }

  private Pair<Polygon, OBB2D> polygon3() {
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
    return new Pair<>(polygon, obb);
  }

  private Pair<Polygon, OBB2D> polygon4() {
    var coordinates =
        new Coordinate[] {
          new Coordinate(300, 300),
          new Coordinate(400, 320),
          new Coordinate(380, 400),
          new Coordinate(320, 380),
          new Coordinate(300, 300)
        };
    var polygon = geometryFactory.createPolygon(coordinates);
    var obb =
        OBB2D
            .builder()
            .center(new LasPointGeometry(340.588, 347.647, 0, BATIMENT))
            .area(8400.000)
            .angle(1.816)
            .width(82.462)
            .height(101.865)
            .build();
    return new Pair<>(polygon, obb);
  }

  private Pair<Polygon, OBB2D> polygon5() {
    var coordinates =
        new Coordinate[] {
          new Coordinate(500, 100),
          new Coordinate(550, 150),
          new Coordinate(500, 200),
          new Coordinate(450, 150),
          new Coordinate(500, 100)
        };
    var polygon = geometryFactory.createPolygon(coordinates);
    var obb =
        OBB2D
            .builder()
            .center(new LasPointGeometry(500.000, 150.000, 0, BATIMENT))
            .area(5000.000)
            .angle(-0.785)
            .width(70.711)
            .height(70.711)
            .build();
    return new Pair<>(polygon, obb);
  }

  private Pair<Polygon, OBB2D> polygon6() {
    var coordinates =
        new Coordinate[] {
          new Coordinate(500, 300),
          new Coordinate(580, 320),
          new Coordinate(560, 400),
          new Coordinate(480, 380),
          new Coordinate(500, 300)
        };
    var polygon = geometryFactory.createPolygon(coordinates);
    var obb =
        OBB2D
            .builder()
            .center(new LasPointGeometry(530.000, 350.000, 0, BATIMENT))
            .area(6800.000)
            .angle(-1.326)
            .width(82.462)
            .height(82.462)
            .build();
    return new Pair<>(polygon, obb);
  }

  private static void assertEquals(OBB2D expected, OBB2D actual) {
    Assertions.assertEquals(expected.area(), actual.area(), EPS);
    Assertions.assertEquals(expected.width(), actual.width(), EPS);
    Assertions.assertEquals(expected.height(), actual.height(), EPS);
    Assertions.assertEquals(expected.angle(), actual.angle(), EPS);
    Assertions.assertEquals(expected.center().getX(), actual.center().getX(), EPS);
    Assertions.assertEquals(expected.center().getY(), actual.center().getY(), EPS);
  }
}
