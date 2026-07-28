package app.bpartners.geojobs.service.cityjson.local;

import static app.bpartners.geojobs.model.geometry.GeometryFactory.geometryFactory;
import static app.bpartners.geojobs.model.lidar.planes.conf.Plane3DExtractorConf.getDefault;
import static app.bpartners.geojobs.model.lidar.planes.model.LasRoofDelimitationType.ROOF_SEGMENT_FACE_DELIMITATION;
import static java.nio.file.Files.createDirectories;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toSet;

import app.bpartners.geojobs.model.lidar.planes.exporter.Plane3DExtractionStepExporter;
import app.bpartners.geojobs.service.cityjson.LidarDataToCityJsonProcessor;
import app.bpartners.geojobs.service.cityjson.factory.CityJsonFactory;
import app.bpartners.geojobs.service.lidar.api.SwissBoundaryChecker;
import app.bpartners.geojobs.utils.lidar.LasRoofsPointsExtractorCreator;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Set;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Polygon;

class CityJSONLocalTestWithoutSegmentationUtils {
  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  static void process(
      String outputFolderName, List<String> pansFiles, Set<String> lasFiles, File directoryOutput) {
    var outputFolder = createOutputFolder(directoryOutput, outputFolderName);
    var pans =
        pansFiles.stream()
            .map(CityJSONLocalTestWithoutSegmentationUtils::readCoordinate)
            .collect(toSet());
    var geometry = geometryFactory.createMultiPolygon(pans.toArray(Polygon[]::new));

    var exporter = new Plane3DExtractionStepExporter(OBJECT_MAPPER, outputFolder, "EPSG:2154", "1");
    var cityJsonProcessor =
        new LidarDataToCityJsonProcessor(
            new CityJsonFactory(directoryOutput), exporter, new SwissBoundaryChecker());
    var lasRoofsPointsExtractor = LasRoofsPointsExtractorCreator.create(lasFiles, Set.of(geometry));

    var data = lasRoofsPointsExtractor.apply(ROOF_SEGMENT_FACE_DELIMITATION, Set.of(geometry));
    cityJsonProcessor.apply(outputFolderName, data, getDefault());
  }

  private static Polygon readCoordinate(String filepath) {
    var fileUrl =
        requireNonNull(
            CityJSONLocalTestWithoutSegmentationUtils.class.getClassLoader().getResource(filepath));
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
