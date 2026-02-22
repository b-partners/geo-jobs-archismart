package app.bpartners.geojobs.service.cityjson.local;

import static app.bpartners.geojobs.model.geometry.GeometryFactory.geometryFactory;
import static app.bpartners.geojobs.model.lidar.planes.Plane3DExtractorConf.getDefault;
import static java.nio.file.Files.createDirectories;
import static java.util.Objects.requireNonNull;

import app.bpartners.geojobs.model.lidar.planes.exporter.Plane3DExtractionStepExporter;
import app.bpartners.geojobs.service.cityjson.LidarDataToCityJsonProcessor;
import app.bpartners.geojobs.service.cityjson.factory.CityJsonFactory;
import app.bpartners.geojobs.utils.lidar.LidarRoofsAnalysisProcessorCreator;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Set;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Polygon;

class CityJSONLocalTestUtils {
  private static final String LAS_FILE_PREFIX = "cityjson/las/";
  private static final String GEO_JSON_FILE_PREFIX = "cityjson/geojson/";
  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  static void process(String filename, List<String> files, File directoryOutput) {
    var filenameWithoutSuffix = filename.substring(0, filename.indexOf('.'));
    var geojson = GEO_JSON_FILE_PREFIX + filename;
    var lasFiles = files.stream().map(file -> LAS_FILE_PREFIX + file).toList();
    var outputFolder = createOutputFolder(directoryOutput, filenameWithoutSuffix);

    var delimitation = readCoordinate(geojson);
    var exporter = new Plane3DExtractionStepExporter(OBJECT_MAPPER, outputFolder, "EPSG:2143", "1");
    var dataProcessorCreator = new LidarRoofsAnalysisProcessorCreator();
    var cityJsonProcessor =
        new LidarDataToCityJsonProcessor(new CityJsonFactory(directoryOutput), exporter);
    var dataProcessor = dataProcessorCreator.create(delimitation, lasFiles);

    var data = dataProcessor.from(Set.of(delimitation));
    cityJsonProcessor.apply(filename.substring(0, filename.indexOf(".")), data, getDefault());
  }

  private static Geometry readCoordinate(String filepath) {
    var fileUrl =
        requireNonNull(CityJSONLocalTestUtils.class.getClassLoader().getResource(filepath));
    var file = new File(fileUrl.getFile());

    try {
      var root = OBJECT_MAPPER.readTree(file);
      var geometry = root.get("geometry");
      var coordinates = geometry.get("coordinates").get(0);
      return toPolygon(coordinates);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private static Polygon toPolygon(JsonNode coordinatesNode) {
    var coordinates = new Coordinate[coordinatesNode.size()];

    for (int i = 0; i < coordinatesNode.size(); i++) {
      var coordinate = coordinatesNode.get(i);
      double lon = coordinate.get(0).asDouble();
      double lat = coordinate.get(1).asDouble();
      coordinates[i] = new Coordinate(lon, lat);
    }

    return geometryFactory.createPolygon(coordinates);
  }

  private static File createOutputFolder(File directoryOutput, String name) {
    try {
      return createDirectories(directoryOutput.toPath().resolve(name)).toFile();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
}
