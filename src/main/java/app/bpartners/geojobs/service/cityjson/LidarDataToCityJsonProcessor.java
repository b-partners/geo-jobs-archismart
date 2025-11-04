package app.bpartners.geojobs.service.cityjson;

import static java.util.UUID.randomUUID;

import app.bpartners.geojobs.service.cityjson.exception.CityJsonException;
import app.bpartners.geojobs.service.cityjson.factory.BuildingGroundPolygonFactory;
import app.bpartners.geojobs.service.cityjson.factory.BuildingWallPolygonFactory;
import app.bpartners.geojobs.service.cityjson.factory.CityJsonFactory;
import app.bpartners.geojobs.service.cityjson.model.BuildingData;
import app.bpartners.geojobs.service.cityjson.model.PolygonWithProperties;
import app.bpartners.geojobs.service.lidar.LidarRoofsAnalysisProcessor;
import app.bpartners.geojobs.service.lidar.model.geometry.LasPointGeometry;
import app.bpartners.geojobs.service.lidar.model.geometry.roof.LidarRoofData;
import app.bpartners.geojobs.service.lidar.model.geometry.roof.RoofPlane3D;
import app.bpartners.geojobs.service.lidar.model.geometry.roof.RoofProperties;
import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class LidarDataToCityJsonProcessor
    implements Function<LidarRoofsAnalysisProcessor.RoofsAnalysisResult, File> {

  private final CityJsonFactory cityJsonFactory;
  private static final String PLANE_SLOPE_KEY = "slope_in_degrees";
  private static final String ROOF_HEIGHT_KEY = "height_in_meters";

  @Override
  public File apply(LidarRoofsAnalysisProcessor.RoofsAnalysisResult roofsAnalysisResults) {
    var buildingsData =
        roofsAnalysisResults.roofsData().values().stream()
            .map(LidarDataToCityJsonProcessor::toBuildingData)
            .toList();

    var id = randomUUID().toString();

    try {
      var cityJsonFile = cityJsonFactory.make(id, id, buildingsData);
      log.info("CityJSON file saved at {}", cityJsonFile.getAbsolutePath());
      return cityJsonFile;
    } catch (CityJsonException e) {
      throw new RuntimeException(e);
    }
  }

  private static BuildingData toBuildingData(LidarRoofData lidarRoofData) {
    var roofProperty = new RoofProperties(lidarRoofData);
    var planes = roofProperty.getPlanes();
    var height = roofProperty.getHeightInMeters().getValue();
    var groundZ =
        roofProperty.getCleanedGroundPoints().stream()
            .mapToDouble(LasPointGeometry::getZ)
            .average()
            .orElse(0);

    var roofs = planes.stream().map(LidarDataToCityJsonProcessor::toPolygonWithProperties).toList();

    var walls =
        planes.stream().map(plane -> createWalls(plane, groundZ)).toList().stream()
            .flatMap(List::stream)
            .toList();

    var grounds = planes.stream().map(plane -> createGround(plane, groundZ)).toList();

    return BuildingData.builder()
        .id(randomUUID().toString())
        .properties(Map.of(ROOF_HEIGHT_KEY, height))
        .roofs(roofs)
        .walls(walls)
        .grounds(grounds)
        .build();
  }

  private static PolygonWithProperties toPolygonWithProperties(RoofPlane3D plane) {
    var slope = plane.getSlopeInDegrees().getValue();
    return PolygonWithProperties.builder()
        .polygon(plane.getDelimitation())
        .properties(Map.of(PLANE_SLOPE_KEY, slope))
        .build();
  }

  private static List<PolygonWithProperties> createWalls(RoofPlane3D plane, double groundZ) {
    var roofPolygon = plane.getDelimitation();
    var wallsPolygons = BuildingWallPolygonFactory.make(roofPolygon, groundZ);

    return wallsPolygons.stream()
        .map(polygon -> new PolygonWithProperties(polygon, Map.of()))
        .toList();
  }

  private static PolygonWithProperties createGround(RoofPlane3D plane, double groundZ) {
    var roofPolygon = plane.getDelimitation();
    var groundPolygon = BuildingGroundPolygonFactory.make(roofPolygon, groundZ);
    return new PolygonWithProperties(groundPolygon, Map.of());
  }
}
