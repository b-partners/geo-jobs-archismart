package app.bpartners.geojobs.model.geometry.lidar.planes.topology.algorithm;

import static app.bpartners.geojobs.file.FileWriter.createTempDirectory;
import static app.bpartners.geojobs.model.lidar.planes.exporter.Plane3DExtractionStep.DEBUG;
import static app.bpartners.geojobs.model.lidar.planes.topology.algorithm.PolygonSmartUnion.union;
import static app.bpartners.geojobs.utils.lidar.LasPointGeometryLoaderUtils.readPointsFromResources;
import static org.junit.jupiter.api.Assertions.assertTrue;

import app.bpartners.geojobs.model.lidar.LasPointGeometry;
import app.bpartners.geojobs.model.lidar.planes.Plane3D;
import app.bpartners.geojobs.model.lidar.planes.PlaneDelimitation;
import app.bpartners.geojobs.model.lidar.planes.algorithm.PlaneFitter;
import app.bpartners.geojobs.model.lidar.planes.conf.RangedConf;
import app.bpartners.geojobs.model.lidar.planes.exporter.Plane3DExtractionStepExporter;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.util.Collection;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Polygon;

@Slf4j
class PolygonSmartUnionTest {
  private static final int TEST_COUNT = 10;
  private static final double MAX_DISTANCE = 1;
  private static final double MIN_INTERSECTION_DISTANCE_DISTANCE = 1;

  private static final File OUTPUT_DIRECTORY = createTempDirectory();
  private static final Plane3DExtractionStepExporter exporter =
      new Plane3DExtractionStepExporter(new ObjectMapper(), OUTPUT_DIRECTORY, "EPSG:2154", "");

  @BeforeAll
  static void setUp() {
    log.info("Output Folder={}", OUTPUT_DIRECTORY.getAbsolutePath());
  }

  @Test
  void test_with_rupture_line_case_1() {
    var points1 = readPointsFromResources("cityjson/topology/rupture_line/case_1/pan_1.geojson");
    var points2 = readPointsFromResources("cityjson/topology/rupture_line/case_1/pan_2.geojson");
    var points3 = readPointsFromResources("cityjson/topology/rupture_line/case_1/pan_3.geojson");
    var points4 = readPointsFromResources("cityjson/topology/rupture_line/case_1/pan_4.geojson");

    for (int i = 0; i < TEST_COUNT; i++) {
      var plane1 = createPlane(points1);
      var plane2 = createPlane(points2);
      var plane3 = createPlane(points3);
      var plane4 = createPlane(points4);
      var firstIter = i == 0;

      // 1-2 & 2-1
      var res12 = getUnion(plane1, plane2);
      var res21 = getUnion(plane2, plane1);
      if (firstIter) {
        res12.ifPresent(p -> export(p, 1, 2));
        res21.ifPresent(p -> export(p, 2, 1));
      }
      assertTrue(res12.isPresent());
      assertTrue(res21.isPresent());

      // 3-4 & 4-3
      var res34 = getUnion(plane3, plane4);
      var res43 = getUnion(plane4, plane3);
      if (firstIter) {
        res34.ifPresent(p -> export(p, 3, 4));
        res43.ifPresent(p -> export(p, 4, 3));
      }
      assertTrue(res34.isPresent());
      assertTrue(res43.isPresent());

      // 1-3 & 1-4 & 3-1 & 4-1
      var res13 = getUnion(plane1, plane3);
      var res31 = getUnion(plane3, plane1);
      var res14 = getUnion(plane1, plane4);
      var res41 = getUnion(plane4, plane1);
      if (firstIter) {
        res13.ifPresent(p -> export(p, 1, 3));
        res31.ifPresent(p -> export(p, 3, 1));
        res14.ifPresent(p -> export(p, 1, 4));
        res41.ifPresent(p -> export(p, 4, 1));
      }
      assertTrue(res13.isEmpty());
      assertTrue(res31.isEmpty());
      assertTrue(res14.isPresent());
      assertTrue(res41.isPresent());

      // 2-3 & 2-4 & 3-2 & 4-2
      var res23 = getUnion(plane2, plane3);
      var res24 = getUnion(plane2, plane4);
      var res32 = getUnion(plane3, plane2);
      var res42 = getUnion(plane4, plane2);
      if (firstIter) {
        res23.ifPresent(p -> export(p, 2, 3));
        res24.ifPresent(p -> export(p, 2, 4));
        res32.ifPresent(p -> export(p, 3, 2));
        res42.ifPresent(p -> export(p, 4, 2));
      }
      assertTrue(res23.isPresent());
      assertTrue(res32.isPresent());
      assertTrue(res24.isEmpty());
      assertTrue(res42.isEmpty());
    }
  }

  @Test
  void test_with_rupture_line_case_2() {
    var points1 = readPointsFromResources("cityjson/topology/rupture_line/case_2/pan_1.geojson");
    var points2 = readPointsFromResources("cityjson/topology/rupture_line/case_2/pan_2.geojson");
    var points3 = readPointsFromResources("cityjson/topology/rupture_line/case_2/pan_3.geojson");

    for (int i = 0; i < TEST_COUNT; i++) {
      var plane1 = createPlane(points1);
      var plane2 = createPlane(points2);
      var plane3 = createPlane(points3);
      var firstIter = i == 0;

      // 1-2 & 2-1
      var res12 = getUnion(plane1, plane2);
      var res21 = getUnion(plane2, plane1);
      if (firstIter) {
        res12.ifPresent(p -> export(p, 1, 2));
        res21.ifPresent(p -> export(p, 2, 1));
      }
      assertTrue(res12.isPresent());
      assertTrue(res21.isPresent());

      // 1-3 & 3-1
      var res13 = getUnion(plane1, plane3);
      var res31 = getUnion(plane3, plane1);
      if (firstIter) {
        res13.ifPresent(p -> export(p, 1, 3));
        res31.ifPresent(p -> export(p, 3, 1));
      }
      assertTrue(res13.isPresent());
      assertTrue(res31.isPresent());

      // 2-3 & 3-2
      var res23 = getUnion(plane2, plane3);
      var res32 = getUnion(plane3, plane2);
      if (firstIter) {
        res23.ifPresent(p -> export(p, 2, 3));
        res32.ifPresent(p -> export(p, 3, 2));
      }
      assertTrue(res23.isPresent());
      assertTrue(res32.isPresent());
    }
  }

  private static void export(Polygon union, int panA, int panB) {
    var subExporter = exporter.subSuffix(String.format("UNION_OF_%d_AND_%d", panA, panB));
    subExporter.export(DEBUG, union);
  }

  private static Optional<Polygon> getUnion(Plane3D a, Plane3D b) {
    return union(
        a.getDelimitation(), b.getDelimitation(), MAX_DISTANCE, MIN_INTERSECTION_DISTANCE_DISTANCE);
  }

  private static Plane3D createPlane(Collection<LasPointGeometry> points) {
    return PlaneFitter.fit(points).toBuilder().delimitationConf(delimitationConf).build();
  }

  private static final PlaneDelimitation.PlaneDelimitationConf delimitationConf =
      PlaneDelimitation.PlaneDelimitationConf.builder()
          .concaveRatio(
              RangedConf.from(
                  new RangedConf.IntegerRangedConf<>(Integer.MIN_VALUE, 200, 0.2),
                  new RangedConf.IntegerRangedConf<>(201, Integer.MAX_VALUE, 0.2)))
          .simplificationEpsilon(0.5)
          .build();
}
