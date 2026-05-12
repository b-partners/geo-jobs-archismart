package app.bpartners.geojobs.model.geometry.lidar.planes.topology;

import static app.bpartners.geojobs.model.lidar.planes.topology.model.RoofRelationType.*;
import static app.bpartners.geojobs.utils.lidar.LasPointGeometryLoaderUtils.readPointsFromResources;
import static org.junit.jupiter.api.Assertions.assertEquals;

import app.bpartners.geojobs.model.lidar.planes.algorithm.PlaneFitter;
import app.bpartners.geojobs.model.lidar.planes.topology.RoofRelationClassifier;
import app.bpartners.geojobs.model.lidar.planes.topology.RoofRelationClassifier.RoofRelationClassifierConf;
import org.junit.jupiter.api.Test;

class RoofRelationClassifierTest {
  private static final RoofRelationClassifier subject =
      new RoofRelationClassifier(RoofRelationClassifierConf.builder().angleThresholdDeg(5).build());
  private static final int TEST_COUNT = 10;

  @Test
  void should_detect_S_relation() {
    var pan1 = readPointsFromResources("cityjson/roofs/roof_2/pan_1.geojson");
    var pan2 = readPointsFromResources("cityjson/roofs/roof_2/pan_2.geojson");

    for (int i = 0; i < TEST_COUNT; i++) {
      var plane1 = PlaneFitter.fit(pan1);
      var plane2 = PlaneFitter.fit(pan2);

      assertEquals(S, subject.apply(plane1, plane2));
    }
  }

  @Test
  void should_detect_O_PLUS_relation() {
    var pan1 = readPointsFromResources("cityjson/roofs/roof_9/pan_1.geojson");
    var pan2 = readPointsFromResources("cityjson/roofs/roof_9/pan_2.geojson");
    var pan3 = readPointsFromResources("cityjson/roofs/roof_9/pan_3.geojson");
    var pan4 = readPointsFromResources("cityjson/roofs/roof_9/pan_4.geojson");

    for (int i = 0; i < TEST_COUNT; i++) {
      var plane1 = PlaneFitter.fit(pan1);
      var plane2 = PlaneFitter.fit(pan2);
      var plane3 = PlaneFitter.fit(pan3);
      var plane4 = PlaneFitter.fit(pan4);

      assertEquals(S, subject.apply(plane1, plane2));
      assertEquals(O_PLUS, subject.apply(plane1, plane3));
      assertEquals(O_PLUS, subject.apply(plane1, plane4));
      assertEquals(O_PLUS, subject.apply(plane2, plane3));
      assertEquals(O_PLUS, subject.apply(plane2, plane4));
    }
  }

  @Test
  void should_detect_O_MINUS_relation() {
    var pan1 = readPointsFromResources("cityjson/roofs/roof_6/pan_1.geojson");
    var pan2 = readPointsFromResources("cityjson/roofs/roof_6/pan_2.geojson");

    for (int i = 0; i < TEST_COUNT; i++) {
      var plane1 = PlaneFitter.fit(pan1);
      var plane2 = PlaneFitter.fit(pan2);

      assertEquals(O_MINUS, subject.apply(plane1, plane2));
    }
  }
}
