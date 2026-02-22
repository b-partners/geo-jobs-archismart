package app.bpartners.geojobs.service.cityjson;

import static app.bpartners.geojobs.file.FileWriter.createTempDirectory;
import static app.bpartners.geojobs.model.geometry.GeometryFactory.geometryFactory;

import app.bpartners.geojobs.model.lidar.planes.Plane3DExtractorConf;
import app.bpartners.geojobs.model.lidar.planes.exporter.Plane3DExtractionStepExporter;
import app.bpartners.geojobs.service.cityjson.factory.CityJsonFactory;
import app.bpartners.geojobs.utils.lidar.LidarRoofsAnalysisProcessorCreator;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;

@Slf4j
class Plane3DExtractionDebuggerTest {
  private static final String LAMBERT_93 = "EPSG:2143";
  private static final File EXPORT_OUTPUT_FOLDER = createTempDirectory();

  private static final Plane3DExtractionStepExporter exporter =
      new Plane3DExtractionStepExporter(new ObjectMapper(), EXPORT_OUTPUT_FOLDER, LAMBERT_93, "1");

  private static final Plane3DExtractorConf conf =
      Plane3DExtractorConf.getDefault().toBuilder().build();

  private static final LidarDataToCityJsonProcessor cityJsonProcessor =
      new LidarDataToCityJsonProcessor(new CityJsonFactory(EXPORT_OUTPUT_FOLDER), exporter);

  private static final LidarRoofsAnalysisProcessorCreator processorCreator =
      new LidarRoofsAnalysisProcessorCreator();

  @Test
  void export() {
    log.info("Output Folder = {}", EXPORT_OUTPUT_FOLDER);
    var roofsGeometries = Set.of(roofGeometry1());
    var processor = processorCreator.create(roofsGeometries);

    var result = processor.from(roofsGeometries);

    cityJsonProcessor.apply("debug_city_jsons", result, conf);
  }

  private static Geometry roofGeometry1() {
    var roof1Coordinates =
        new Coordinate[] {
          new Coordinate(2.243891733457616, 48.82448842864014),
          new Coordinate(2.243947393505863, 48.82437718542337),
          new Coordinate(2.244038835011281, 48.82440597780899),
          new Coordinate(2.2440209442821413, 48.82445309258651),
          new Coordinate(2.244197863717403, 48.8244975898354),
          new Coordinate(2.24422768160008, 48.82447010624497),
          new Coordinate(2.24432906240051, 48.824487119898066),
          new Coordinate(2.244263463059525, 48.82456695311532),
          new Coordinate(2.243891733457616, 48.82448842864014)
        };
    return geometryFactory.createPolygon(roof1Coordinates);
  }
}
