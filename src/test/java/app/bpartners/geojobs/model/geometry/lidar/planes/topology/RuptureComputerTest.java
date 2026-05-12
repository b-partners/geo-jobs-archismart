package app.bpartners.geojobs.model.geometry.lidar.planes.topology;

import static app.bpartners.geojobs.utils.lidar.LasPointGeometryLoaderUtils.readPointsFromResources;
import static app.bpartners.geojobs.utils.lidar.Plane3DExtractorConfCreator.planeDelimitationConf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import app.bpartners.geojobs.model.lidar.LasPointGeometry;
import app.bpartners.geojobs.model.lidar.planes.Plane3D;
import app.bpartners.geojobs.model.lidar.planes.PlaneDelimitation.PlaneDelimitationConf;
import app.bpartners.geojobs.model.lidar.planes.algorithm.PlaneFitter;
import app.bpartners.geojobs.model.lidar.planes.topology.RuptureComputer;
import java.util.Collection;
import org.junit.jupiter.api.Test;

class RuptureComputerTest {
  private static final RuptureComputer subject = new RuptureComputer();
  private static final PlaneDelimitationConf delimitationConf = planeDelimitationConf();
  private static final int TEST_COUNT = 10;

  @Test
  void case_1() {
    var points1 = readPointsFromResources("cityjson/roofs/roof_6/pan_3.geojson");
    var points2 = readPointsFromResources("cityjson/roofs/roof_6/pan_4.geojson");
    var points3 = readPointsFromResources("cityjson/roofs/roof_6/pan_5.geojson");
    var points4 = readPointsFromResources("cityjson/roofs/roof_6/pan_6.geojson");

    for (int i = 0; i < TEST_COUNT; i++) {
      var plane1 = createPlane(points1);
      var plane2 = createPlane(points2);
      var plane3 = createPlane(points3);
      var plane4 = createPlane(points4);

      // 1-2 & 2-1
      assertTrue(subject.apply(plane1, plane2).isPresent());
      assertTrue(subject.apply(plane2, plane1).isPresent());

      // 3-4 & 4-3
      assertTrue(subject.apply(plane3, plane4).isPresent());
      assertTrue(subject.apply(plane4, plane3).isPresent());

      // 1-3 & 1-4 & 3-1 & 4-1
      assertTrue(subject.apply(plane1, plane3).isEmpty());
      assertTrue(subject.apply(plane1, plane4).isEmpty());
      assertTrue(subject.apply(plane3, plane1).isEmpty());
      assertTrue(subject.apply(plane4, plane1).isEmpty());

      // 2-3 & 2-4 & 3-2 & 4-2
      assertTrue(subject.apply(plane2, plane3).isEmpty());
      assertTrue(subject.apply(plane2, plane4).isEmpty());
      assertTrue(subject.apply(plane3, plane2).isEmpty());
      assertTrue(subject.apply(plane4, plane2).isEmpty());
    }
  }

  @Test
  void case_2() {
    var points1 = readPointsFromResources("cityjson/roofs/roof_7/pan_1.geojson");
    var points2 = readPointsFromResources("cityjson/roofs/roof_7/pan_2.geojson");
    var points3 = readPointsFromResources("cityjson/roofs/roof_7/pan_3.geojson");

    for (int i = 0; i < TEST_COUNT; i++) {
      var plane1 = createPlane(points1);
      var plane2 = createPlane(points2);
      var plane3 = createPlane(points3);

      // 1-2 & 2-1
      assertTrue(subject.apply(plane1, plane2).isPresent());
      assertTrue(subject.apply(plane2, plane1).isPresent());

      // 1-3 & 3-1
      assertTrue(subject.apply(plane1, plane3).isPresent());
      assertTrue(subject.apply(plane3, plane1).isPresent());

      // 2-3 & 3-2
      assertTrue(subject.apply(plane2, plane3).isEmpty());
      assertTrue(subject.apply(plane3, plane2).isEmpty());
    }
  }

  static Plane3D createPlane(Collection<LasPointGeometry> points) {
    return PlaneFitter.fit(points).toBuilder().delimitationConf(delimitationConf).build();
  }
}
