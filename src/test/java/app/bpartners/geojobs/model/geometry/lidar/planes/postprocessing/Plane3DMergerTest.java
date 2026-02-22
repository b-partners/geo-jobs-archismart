package app.bpartners.geojobs.model.geometry.lidar.planes.postprocessing;

import static app.bpartners.geojobs.model.geometry.GeometryFactory.geometryFactory;
import static org.junit.jupiter.api.Assertions.assertEquals;

import app.bpartners.geojobs.model.lidar.planes.Plane3D;
import app.bpartners.geojobs.model.lidar.planes.postprocessing.Plane3DMerger;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Polygon;

class Plane3DMergerTest {
  private static final double EPSILON_SLOPE = 7;
  private static final double EPSILON_Z_DISTANCE = 0.3;
  private static final double EPSILON_XY_DISTANCE = 0.5;
  private static final double CONCAVE_RATIO = 0.2;

  private static final Plane3DMerger subject =
      new Plane3DMerger(CONCAVE_RATIO, EPSILON_SLOPE, EPSILON_Z_DISTANCE, EPSILON_XY_DISTANCE);

  @Test
  void should_not_merge_if_z_not_closed() {
    var poly1 = square(new Coordinate(0, 0, 0), 1);
    var poly2 = square(new Coordinate(0, 0, 1), 1);

    var plane1 = plane(0, 0, 1, 0, poly1);
    var plane2 = plane(0, 0, 1, 1, poly2);

    var merged = subject.apply(Set.of(plane1, plane2));

    assertEquals(2, merged.size());
  }

  @Test
  void should_not_merge_if_xy_not_closed() {
    var poly1 = square(new Coordinate(0, 0, 1), 1);
    var poly2 = square(new Coordinate(5, 0, 1), 1);

    var plane1 = plane(0, 0, 1, 0, poly1);
    var plane2 = plane(0, 0, 1, 5, poly2);

    var merged = subject.apply(Set.of(plane1, plane2));

    assertEquals(2, merged.size());
  }

  @Test
  void should_not_merge_if_inclined_too_much() {
    var flatSquare = square(new Coordinate(0, 0, 0), 1);

    double tan30 = Math.tan(Math.toRadians(30));
    var inclinedCoordinates =
        new Coordinate[] {
          new Coordinate(0, 0, 0),
          new Coordinate(1, 0, 1 * tan30),
          new Coordinate(1, 1, 1 * tan30),
          new Coordinate(0, 1, 0),
          new Coordinate(0, 0, 0)
        };
    var inclinedSquare = geometryFactory.createPolygon(inclinedCoordinates);

    var plane1 = planeFromCoordinates(flatSquare);
    var plane2 = planeFromCoordinates(inclinedSquare);

    var merged = subject.apply(Set.of(plane1, plane2));
    assertEquals(2, merged.size());
  }

  @Test
  void should_merge_two_flat_planes() {
    var square1 = square(new Coordinate(0, 0, 0), 1);
    var square2 = square(new Coordinate(0.4, 0.4, 0), 1);

    var plane1 = planeFromCoordinates(square1);
    var plane2 = planeFromCoordinates(square2);

    var merged = subject.apply(Set.of(plane1, plane2));
    assertEquals(1, merged.size());
  }

  @Test
  void should_merge_two_parallel_inclined_planes() {
    double angle1 = Math.toRadians(20);
    double tan20 = Math.tan(angle1);

    double angle2 = Math.toRadians(13);
    double tan19 = Math.tan(angle2);

    var coordinates1 =
        new Coordinate[] {
          new Coordinate(0, 0, 0),
          new Coordinate(1, 0, 1 * tan20),
          new Coordinate(1, 1, 1 * tan20),
          new Coordinate(0, 1, 0),
          new Coordinate(0, 0, 0)
        };
    var coordinates2 =
        new Coordinate[] {
          new Coordinate(0.2, 0.2, 0),
          new Coordinate(1.2, 0.2, 1 * tan19),
          new Coordinate(1.2, 1.2, 1 * tan19),
          new Coordinate(0.2, 1.2, 0),
          new Coordinate(0.2, 0.2, 0)
        };

    var square1 = geometryFactory.createPolygon(coordinates1);
    var square2 = geometryFactory.createPolygon(coordinates2);

    var plane1 = planeFromCoordinates(square1);
    var plane2 = planeFromCoordinates(square2);

    var merged = subject.apply(Set.of(plane1, plane2));
    assertEquals(1, merged.size());
  }

  private static Plane3D planeFromCoordinates(Polygon delimitation) {
    var coordinates = delimitation.getCoordinates();
    var p1 = coordinates[0];
    var p2 = coordinates[1];
    var p3 = coordinates[2];

    double vx = p2.getX() - p1.getX();
    double vy = p2.getY() - p1.getY();
    double vz = p2.getZ() - p1.getZ();

    double ux = p3.getX() - p1.getX();
    double uy = p3.getY() - p1.getY();
    double uz = p3.getZ() - p1.getZ();

    double a = vy * uz - vz * uy;
    double b = vz * ux - vx * uz;
    double c = vx * uy - vy * ux;

    double d = -(a * p1.getX() + b * p1.getY() + c * p1.getZ());
    return Plane3D.builder()
        .a(a)
        .b(b)
        .c(c)
        .points(Set.of())
        .d(d)
        .delimitation(delimitation)
        .build();
  }

  private static Plane3D plane(double a, double b, double c, double d, Polygon delimitation) {
    return Plane3D.builder().a(a).b(b).c(c).d(d).delimitation(delimitation).build();
  }

  private static Polygon square(Coordinate origin, double width) {
    double x = origin.getX();
    double y = origin.getY();
    double z = origin.getZ();

    var coordinates =
        new Coordinate[] {
          new Coordinate(x, y, z),
          new Coordinate(x + width, y, z),
          new Coordinate(x + width, y + width, z),
          new Coordinate(x, y + width, z),
          new Coordinate(x, y, z)
        };

    return geometryFactory.createPolygon(coordinates);
  }
}
