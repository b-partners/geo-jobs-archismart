package app.bpartners.geojobs.model.geometry.lidar.planes.topology;

import static app.bpartners.geojobs.model.geometry.lidar.planes.topology.RuptureComputerTest.createPlane;
import static app.bpartners.geojobs.model.lidar.planes.topology.model.RoofRelationType.*;
import static app.bpartners.geojobs.utils.lidar.LasPointGeometryLoaderUtils.readPointsFromResources;
import static org.junit.jupiter.api.Assertions.*;

import app.bpartners.geojobs.model.lidar.planes.topology.RoofRelationClassifier.RoofRelationClassifierConf;
import app.bpartners.geojobs.model.lidar.planes.topology.RoofTopologyBuilder;
import java.util.List;
import org.junit.jupiter.api.Test;

class RoofTopologyBuilderTest {
  private static final RoofRelationClassifierConf conf =
      RoofRelationClassifierConf.builder().angleThresholdDeg(5).build();
  private static final RoofTopologyBuilder subject = new RoofTopologyBuilder(conf);
  private static final int TEST_COUNT = 10;

  @Test
  void case_1() {
    var points1 = readPointsFromResources("cityjson/roofs/roof_1/pan_1.geojson");
    var plane1 = createPlane(points1);
    assertDoesNotThrow(() -> subject.apply(List.of(plane1)));
  }

  @Test
  void case_2() {
    var points1 = readPointsFromResources("cityjson/roofs/roof_2/pan_1.geojson");
    var points2 = readPointsFromResources("cityjson/roofs/roof_2/pan_2.geojson");
    var points3 = readPointsFromResources("cityjson/roofs/roof_2/pan_3.geojson");

    for (int i = 0; i < TEST_COUNT; i++) {
      var plane1 = createPlane(points1);
      var plane2 = createPlane(points2);
      var plane3 = createPlane(points3);

      var actual = subject.apply(List.of(plane1, plane2, plane3));

      assertTrue(actual.getAdjacency()[0][1]);
      assertTrue(actual.getAdjacency()[1][0]);
      assertFalse(actual.getAdjacency()[0][2]);
      assertFalse(actual.getAdjacency()[2][0]);
      assertFalse(actual.getAdjacency()[1][2]);
      assertFalse(actual.getAdjacency()[2][1]);

      assertEquals(S, actual.getRelations()[0][1]);
      assertEquals(S, actual.getRelations()[1][0]);
      assertEquals(NONE, actual.getRelations()[0][2]);
      assertEquals(NONE, actual.getRelations()[2][0]);
      assertEquals(NONE, actual.getRelations()[1][2]);
      assertEquals(NONE, actual.getRelations()[2][1]);
    }
  }

  @Test
  void case_3() {
    var points1 = readPointsFromResources("cityjson/roofs/roof_7/pan_1.geojson");
    var points2 = readPointsFromResources("cityjson/roofs/roof_7/pan_2.geojson");
    var points3 = readPointsFromResources("cityjson/roofs/roof_7/pan_3.geojson");
    var points4 = readPointsFromResources("cityjson/roofs/roof_7/pan_4.geojson");

    for (int i = 0; i < TEST_COUNT; i++) {
      var plane1 = createPlane(points1);
      var plane2 = createPlane(points2);
      var plane3 = createPlane(points3);
      var plane4 = createPlane(points4);

      var actual = subject.apply(List.of(plane1, plane2, plane3, plane4));

      // 0
      assertTrue(actual.getAdjacency()[0][1]);
      assertTrue(actual.getAdjacency()[0][2]);
      assertFalse(actual.getAdjacency()[0][3]);
      assertEquals(S, actual.getRelations()[0][1]);
      assertEquals(S, actual.getRelations()[0][2]);
      assertEquals(NONE, actual.getRelations()[0][3]);

      // 1
      assertFalse(actual.getAdjacency()[1][2]);
      assertFalse(actual.getAdjacency()[1][3]);

      // 2
      assertFalse(actual.getAdjacency()[2][3]);
    }
  }
}
