package app.bpartners.geojobs.model.geometry.lidar.planes;

import static app.bpartners.geojobs.file.FileWriter.createTempDirectory;
import static app.bpartners.geojobs.model.geometry.GeometryFactory.geometryFactory;
import static app.bpartners.geojobs.utils.lidar.LasPointGeometryLoaderUtils.readPointsFromResources;
import static org.junit.jupiter.api.Assertions.assertTrue;

import app.bpartners.geojobs.model.lidar.planes.PlaneDelimitation;
import app.bpartners.geojobs.model.lidar.planes.PlaneDelimitation.PlaneDelimitationConf;
import app.bpartners.geojobs.model.lidar.planes.conf.RangedConf;
import app.bpartners.geojobs.model.lidar.planes.conf.RangedConf.IntegerRangedConf;
import app.bpartners.geojobs.model.lidar.planes.exporter.Plane3DExtractionStepExporter;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Polygon;

@Slf4j
class PlaneDelimitationIT {
  private static final File directory = createTempDirectory();
  private static final Plane3DExtractionStepExporter exporter =
      new Plane3DExtractionStepExporter(new ObjectMapper(), directory, "EPSG:2143", "");
  private static final PlaneDelimitationConf conf =
      PlaneDelimitationConf.builder()
          .concaveRatio(
              RangedConf.from(
                  new IntegerRangedConf<>(Integer.MIN_VALUE, 200, 0.2),
                  new IntegerRangedConf<>(201, Integer.MAX_VALUE, 0.08)))
          .simplificationEpsilon(0.3)
          .build();

  @BeforeAll
  static void setup() {
    log.info("Output Folder = {}", directory.getAbsolutePath());
  }

  @Test
  void fromConcavePoints() {
    var points = readPointsFromResources("cityjson/points/concave_points_1.geojson");

    var subExporter = exporter.subSuffix("CASE_1");
    var delimitation = new PlaneDelimitation(conf, points, subExporter);
    var actual = delimitation.getPolygon();
    var expected = expectedCase1();

    assertTrue(expected.equalsExact(actual, 0.2));
  }

  @Test
  void fromConcavePoints2() {
    var points = readPointsFromResources("cityjson/points/concave_points_2.geojson");

    var subExporter = exporter.subSuffix("CASE_2");
    var delimitation = new PlaneDelimitation(conf, points, subExporter);
    var actual = delimitation.getPolygon();

    var expected = expectedCase2();

    assertTrue(expected.equalsExact(actual, 0.2));
  }

  @Test
  void fromConcavePoints3() {
    var points = readPointsFromResources("cityjson/points/concave_points_3.geojson");

    var subExporter = exporter.subSuffix("CASE_3");
    var delimitation = new PlaneDelimitation(conf, points, subExporter);
    var actual = delimitation.getPolygon();
    var expected = expectedCase3();

    assertTrue(expected.equalsExact(actual, 0.2));
  }

  private static Polygon expectedCase1() {
    var coordinates =
        new Coordinate[] {
          new Coordinate(379908.92, 6646658.19),
          new Coordinate(379908.62, 6646658.2700000005),
          new Coordinate(379911.21, 6646664.11),
          new Coordinate(379916.49, 6646661.73),
          new Coordinate(379915.91000000003, 6646659.2700000005),
          new Coordinate(379912.52, 6646651.640000001),
          new Coordinate(379908.69, 6646647.390000001),
          new Coordinate(379908.68, 6646653.99),
          new Coordinate(379909.52, 6646654.41),
          new Coordinate(379910.43, 6646656.88),
          new Coordinate(379909.27, 6646657.86),
          new Coordinate(379908.92, 6646658.19)
        };

    return geometryFactory.createPolygon(coordinates);
  }

  private static Polygon expectedCase2() {
    var coordinates =
        new Coordinate[] {
          new Coordinate(379908.92, 6646658.19),
          new Coordinate(379908.62, 6646658.2700000005),
          new Coordinate(379911.21, 6646664.11),
          new Coordinate(379913.24, 6646663.390000001),
          new Coordinate(379913.71, 6646662.75),
          new Coordinate(379916.34, 6646662.05),
          new Coordinate(379916.67, 6646661.05),
          new Coordinate(379912.52, 6646651.640000001),
          new Coordinate(379908.69, 6646647.390000001),
          new Coordinate(379911.33, 6646651.07),
          new Coordinate(379910.15, 6646652.7700000005),
          new Coordinate(379909.41000000003, 6646652.69),
          new Coordinate(379908.74, 6646653.5200000005),
          new Coordinate(379910.43, 6646656.88),
          new Coordinate(379909.27, 6646657.86),
          new Coordinate(379908.92, 6646658.19)
        };

    return geometryFactory.createPolygon(coordinates);
  }

  private static Polygon expectedCase3() {
    var coordinates =
        new Coordinate[] {
          new Coordinate(644488.99, 6858534.98),
          new Coordinate(644490.26, 6858536.03),
          new Coordinate(644514.14, 6858543.350000001),
          new Coordinate(644516.02, 6858542.86),
          new Coordinate(644520.17, 6858535.21),
          new Coordinate(644515.31, 6858533.890000001),
          new Coordinate(644513.9, 6858533.92),
          new Coordinate(644513.41, 6858535.350000001),
          new Coordinate(644511.89, 6858536.51),
          new Coordinate(644499.29, 6858532.17),
          new Coordinate(644498.64, 6858532.3),
          new Coordinate(644497.36, 6858531.0200000005),
          new Coordinate(644498.56, 6858526.95),
          new Coordinate(644497.6900000001, 6858525.71),
          new Coordinate(644495.5700000001, 6858524.68),
          new Coordinate(644492.21, 6858524.28),
          new Coordinate(644489.01, 6858534.23),
          new Coordinate(644488.99, 6858534.98)
        };

    return geometryFactory.createPolygon(coordinates);
  }
}
