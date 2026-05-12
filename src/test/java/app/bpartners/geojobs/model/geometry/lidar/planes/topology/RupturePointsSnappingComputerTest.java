package app.bpartners.geojobs.model.geometry.lidar.planes.topology;

import static app.bpartners.geojobs.model.lidar.planes.topology.model.RoofRelationType.NONE;
import static app.bpartners.geojobs.utils.lidar.LasPointGeometryLoaderUtils.readPointsFromResources;
import static org.junit.jupiter.api.Assertions.assertEquals;

import app.bpartners.geojobs.model.lidar.LasPointGeometry;
import app.bpartners.geojobs.model.lidar.planes.topology.RoofRelationClassifier.RoofRelationClassifierConf;
import app.bpartners.geojobs.model.lidar.planes.topology.RoofTopologyBuilder;
import app.bpartners.geojobs.model.lidar.planes.topology.RupturePointsComputer;
import app.bpartners.geojobs.model.lidar.planes.topology.RupturePointsSnappingComputer;
import app.bpartners.geojobs.model.lidar.planes.topology.model.RoofTopology;
import java.util.Collection;
import java.util.List;
import org.junit.jupiter.api.Test;

class RupturePointsSnappingComputerTest {
  private static final int TEST_COUNT = 10;
  private static final RupturePointsComputer rupturePointsComputer = new RupturePointsComputer();
  private static final RupturePointsSnappingComputer subject = new RupturePointsSnappingComputer();
  private static final RoofTopologyBuilder topologyBuilder =
      new RoofTopologyBuilder(RoofRelationClassifierConf.builder().angleThresholdDeg(5).build());

  @Test
  void roof_6() {
    var pan1 = readPointsFromResources("cityjson/roofs/roof_6/pan_1.geojson");
    var pan2 = readPointsFromResources("cityjson/roofs/roof_6/pan_2.geojson");
    var pan7 = readPointsFromResources("cityjson/roofs/roof_6/pan_7.geojson");

    for (int i = 0; i < TEST_COUNT; i++) {
      var topology = getTopology(List.of(pan1, pan2, pan7));

      var ruptureA = topology.getRuptures()[0][1];
      assertEquals(1, ruptureA.getStartIntersection().size());
      assertEquals(1, ruptureA.getEndIntersection().size());

      var ruptureB = topology.getRuptures()[1][2];
      var startBSize = ruptureB.getStartIntersection().size();
      var endBSize = ruptureB.getEndIntersection().size();
      assertEquals(0, Math.min(startBSize, endBSize));
      assertEquals(1, Math.max(startBSize, endBSize));

      var ruptureC = topology.getRuptures()[2][1];
      var startCSize = ruptureC.getStartIntersection().size();
      var endCSize = ruptureC.getEndIntersection().size();
      assertEquals(0, Math.min(startCSize, endCSize));
      assertEquals(1, Math.max(startCSize, endCSize));

      assertEquals(NONE, topology.getRelations()[0][2]);
    }
  }

  @Test
  void roof_9() {
    var pan1 = readPointsFromResources("cityjson/roofs/roof_9/pan_1.geojson");
    var pan2 = readPointsFromResources("cityjson/roofs/roof_9/pan_2.geojson");
    var pan3 = readPointsFromResources("cityjson/roofs/roof_9/pan_3.geojson");
    var pan4 = readPointsFromResources("cityjson/roofs/roof_9/pan_4.geojson");

    for (int i = 0; i < TEST_COUNT; i++) {
      var topology = getTopology(List.of(pan1, pan2, pan3, pan4));

      var ruptureA = topology.getRuptures()[0][1];
      assertEquals(2, ruptureA.getStartIntersection().size());
      assertEquals(2, ruptureA.getEndIntersection().size());

      var ruptureB = topology.getRuptures()[1][0];
      assertEquals(2, ruptureB.getStartIntersection().size());
      assertEquals(2, ruptureB.getEndIntersection().size());

      var ruptureC = topology.getRuptures()[0][2];
      assertEquals(1, ruptureC.getStartIntersection().size());
      assertEquals(1, ruptureC.getEndIntersection().size());

      var ruptureCPrime = topology.getRuptures()[2][0];
      assertEquals(1, ruptureCPrime.getStartIntersection().size());
      assertEquals(1, ruptureCPrime.getEndIntersection().size());

      var ruptureD = topology.getRuptures()[0][3];
      assertEquals(1, ruptureD.getStartIntersection().size());
      assertEquals(1, ruptureD.getEndIntersection().size());

      var ruptureDPrime = topology.getRuptures()[3][0];
      assertEquals(1, ruptureDPrime.getStartIntersection().size());
      assertEquals(1, ruptureDPrime.getEndIntersection().size());

      var ruptureE = topology.getRuptures()[1][2];
      assertEquals(1, ruptureE.getStartIntersection().size());
      assertEquals(1, ruptureE.getEndIntersection().size());

      var ruptureEPrime = topology.getRuptures()[2][1];
      assertEquals(1, ruptureEPrime.getStartIntersection().size());
      assertEquals(1, ruptureEPrime.getEndIntersection().size());

      var ruptureF = topology.getRuptures()[1][3];
      assertEquals(1, ruptureF.getStartIntersection().size());
      assertEquals(1, ruptureF.getEndIntersection().size());

      var ruptureFPrime = topology.getRuptures()[3][1];
      assertEquals(1, ruptureFPrime.getStartIntersection().size());
      assertEquals(1, ruptureFPrime.getEndIntersection().size());

      assertEquals(NONE, topology.getRelations()[2][3]);
    }
  }

  private static RoofTopology getTopology(List<Collection<LasPointGeometry>> pans) {
    var planes = pans.stream().map(RuptureComputerTest::createPlane).toList();
    var topology = topologyBuilder.apply(planes);
    rupturePointsComputer.accept(planes, topology);
    subject.accept(planes, topology);
    return topology;
  }
}
