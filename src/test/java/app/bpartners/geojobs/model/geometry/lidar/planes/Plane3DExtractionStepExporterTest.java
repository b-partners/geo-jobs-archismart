package app.bpartners.geojobs.model.geometry.lidar.planes;

import static app.bpartners.geojobs.file.FileWriter.createTempDirectory;
import static app.bpartners.geojobs.model.geometry.GeometryFactory.geometryFactory;
import static app.bpartners.geojobs.model.lidar.planes.exporter.Plane3DExtractionStep.INIT_POINTS;
import static app.bpartners.geojobs.model.lidar.planes.exporter.Plane3DExtractionStep.RAW_PLANE_EXTRACTION;
import static app.bpartners.geojobs.service.lidar.model.LidarClass.BATIMENT;
import static org.junit.jupiter.api.Assertions.assertTrue;

import app.bpartners.geojobs.model.lidar.LasPointGeometry;
import app.bpartners.geojobs.model.lidar.planes.exporter.Plane3DExtractionStepExporter;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Polygon;

class Plane3DExtractionStepExporterTest {
  private static final File directory = createTempDirectory();
  private static final Plane3DExtractionStepExporter subject =
      new Plane3DExtractionStepExporter(new ObjectMapper(), directory, "EPSG:2143", "original");

  @Test
  void export_las_points() {
    var expectedFileName = INIT_POINTS.toFilePrefix() + "_original.geojson";
    var path = directory.toPath().resolve(expectedFileName);

    subject.export(INIT_POINTS, points());

    assertTrue(path.toFile().exists());
  }

  @Test
  void export_polygon_points() {
    var expectedFileName = RAW_PLANE_EXTRACTION.toFilePrefix() + "_original.geojson";
    var path = directory.toPath().resolve(expectedFileName);

    subject.export(RAW_PLANE_EXTRACTION, polygon());

    assertTrue(path.toFile().exists());
  }

  @Test
  void export_with_sub_suffix() {
    var exporter = subject.subSuffix("sub_suffix");
    var expectedFileName = RAW_PLANE_EXTRACTION.toFilePrefix() + "_original_sub_suffix.geojson";
    var path = directory.toPath().resolve(expectedFileName);

    exporter.export(RAW_PLANE_EXTRACTION, points());

    assertTrue(path.toFile().exists());
  }

  private static Set<LasPointGeometry> points() {
    return Set.of(new LasPointGeometry(1, 1, 1, BATIMENT), new LasPointGeometry(2, 2, 2, BATIMENT));
  }

  private static Polygon polygon() {
    var coordinates =
        new Coordinate[] {
          new Coordinate(0, 0, 1),
          new Coordinate(1, 0, 1),
          new Coordinate(1, 1, 1),
          new Coordinate(0, 1, 1),
          new Coordinate(0, 0, 1)
        };
    return geometryFactory.createPolygon(coordinates);
  }
}
