package app.bpartners.geojobs.model.geometry.lidar.planes.postprocessing;

import static app.bpartners.geojobs.file.FileWriter.createTempDirectory;
import static app.bpartners.geojobs.utils.lidar.LasPointGeometryLoaderUtils.readPointsFromResources;
import static org.junit.jupiter.api.Assertions.assertEquals;

import app.bpartners.geojobs.model.lidar.planes.PlaneDelimitation;
import app.bpartners.geojobs.model.lidar.planes.conf.Plane3DExtractorConf;
import app.bpartners.geojobs.model.lidar.planes.exporter.Plane3DExtractionStepExporter;
import app.bpartners.geojobs.model.lidar.planes.postprocessing.PolygonSkinnyArmRemover;
import app.bpartners.geojobs.model.lidar.planes.postprocessing.SkinnyArmPointFilter;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

@Slf4j
@Disabled
class SkinnyArmPointFilterTest {
  private static final PolygonSkinnyArmRemover.PolygonSkinnyArmRemoverConf conf =
      Plane3DExtractorConf.getDefault().polygonSkinnyArmRemoverConf();
  private static final PlaneDelimitation.PlaneDelimitationConf delimitationConf =
      Plane3DExtractorConf.getDefault().planeDelimitationConf();
  private static final File BASE_OUTPUT_FOLDER = createTempDirectory();
  private static final Plane3DExtractionStepExporter exporter =
      new Plane3DExtractionStepExporter(new ObjectMapper(), BASE_OUTPUT_FOLDER, "EPSG:2154", "");
  private static final SkinnyArmPointFilter subject =
      new SkinnyArmPointFilter(conf, delimitationConf);

  @BeforeAll
  static void setup() {
    log.info("Output Folder = {}", BASE_OUTPUT_FOLDER.getAbsolutePath());
  }

  @Test
  void should_remove_skinny_arms() {
    var points = readPointsFromResources("cityjson/points/concave_points_4.geojson");

    var subExporter = exporter.subSuffix("CASE_1");
    var actual = subject.apply(points, subExporter);

    assertEquals(947, actual.size());
  }

  @Test
  void should_remove_skinny_arms_2() {
    var points = readPointsFromResources("cityjson/points/concave_points_5.geojson");

    var subExporter = exporter.subSuffix("CASE_2");
    var actual = subject.apply(points, subExporter);

    assertEquals(794, actual.size());
  }
}
