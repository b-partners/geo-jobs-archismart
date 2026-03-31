package app.bpartners.geojobs.model.geometry.lidar.planes.postprocessing;

import static app.bpartners.geojobs.model.geometry.GeometryFactory.geometryFactory;
import static org.junit.jupiter.api.Assertions.*;

import app.bpartners.geojobs.model.lidar.planes.Plane3D;
import app.bpartners.geojobs.model.lidar.planes.postprocessing.RoofBoundaryClipper;
import app.bpartners.geojobs.model.lidar.planes.postprocessing.model.ChimneyPlane3D;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Polygon;

class RoofBoundaryClipperTest {
  private static final Polygon roofDelimitation = roofDelimitation();
  private static final RoofBoundaryClipper subject = new RoofBoundaryClipper(roofDelimitation);

  @Test
  void should_clip_plane_exceeding_boundary() {
    var plane = createPlane(new double[][] {{0, 0}, {5, 0}, {5, 15}, {0, 15}});

    var actual = subject.apply(List.of(plane)).getFirst();

    assertTrue(plane.getDelimitation().getArea() >= 50.0);
    assertTrue(actual.getDelimitation().getArea() <= 50.0);
  }

  @Test
  void should_not_clip_chimney() {
    var plane = createPlane(new double[][] {{8, 8}, {12, 8}, {12, 12}, {8, 12}});
    var chimney = new ChimneyPlane3D(plane);

    var actual = subject.apply(List.of(chimney)).getFirst();

    assertSame(chimney, actual);
  }

  @Test
  void should_ignore_small_planes() {
    var plane = createPlane(new double[][] {{1, 1}, {2, 1}, {2, 2}, {1, 2}});
    var actual = subject.apply(List.of(plane)).getFirst();

    assertSame(plane, actual);
  }

  private static Polygon createPolygon(double[][] coordinates) {
    Coordinate[] result = new Coordinate[coordinates.length + 1];
    for (int i = 0; i < coordinates.length; i++) {
      result[i] = new Coordinate(coordinates[i][0], coordinates[i][1]);
    }
    result[coordinates.length] = result[0];
    return geometryFactory.createPolygon(result);
  }

  private static Plane3D createPlane(double[][] coordinates) {
    return Plane3D.builder()
        .a(0)
        .b(0)
        .c(1)
        .d(0)
        .delimitation(createPolygon(coordinates))
        .points(Set.of())
        .build();
  }

  private static Polygon roofDelimitation() {
    return createPolygon(new double[][] {{0, 0}, {10, 0}, {10, 10}, {0, 10}});
  }
}
