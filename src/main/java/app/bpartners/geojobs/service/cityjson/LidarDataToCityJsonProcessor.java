package app.bpartners.geojobs.service.cityjson;

import static app.bpartners.geojobs.model.lidar.planes.model.LasRoofDelimitationType.ROOF_SEGMENT_FACE_DELIMITATION;
import static app.bpartners.geojobs.service.lidar.utils.MathUtilities.round2;
import static java.util.UUID.randomUUID;

import app.bpartners.geojobs.model.lidar.LasPointGeometry;
import app.bpartners.geojobs.model.lidar.planes.conf.Plane3DExtractorConf;
import app.bpartners.geojobs.model.lidar.planes.exporter.Plane3DExtractionStepExporter;
import app.bpartners.geojobs.model.lidar.planes.model.DelimitedRoofPoints;
import app.bpartners.geojobs.service.cityjson.exception.CityJsonException;
import app.bpartners.geojobs.service.cityjson.factory.BuildingGroundPolygonFactory;
import app.bpartners.geojobs.service.cityjson.factory.BuildingWallPolygonFactory;
import app.bpartners.geojobs.service.cityjson.factory.CityJsonFactory;
import app.bpartners.geojobs.service.cityjson.model.BuildingData;
import app.bpartners.geojobs.service.lidar.LidarRoofsAnalysisProcessor;
import app.bpartners.geojobs.service.lidar.PointsExtractionResult;
import app.bpartners.geojobs.service.lidar.model.geometry.GeometryWithProperties;
import app.bpartners.geojobs.service.lidar.model.geometry.roof.Building3DProperties;
import app.bpartners.geojobs.service.lidar.model.geometry.roof.RoofPlane3D;
import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class LidarDataToCityJsonProcessor
    implements BiFunction<String, PointsExtractionResult, File> {
  private final CityJsonFactory cityJsonFactory;
  private final Plane3DExtractionStepExporter exporter;

  private static final String AREA_KEY = "area_in_square_meters";
  private static final String PLANE_SLOPE_KEY = "slope_in_degrees";
  private static final String DISTANCE_2D_SCALE = "distance_2d_scale";

  @Autowired
  public LidarDataToCityJsonProcessor(CityJsonFactory cityJsonFactory) {
    this.exporter = null;
    this.cityJsonFactory = cityJsonFactory;
  }

  @Deprecated
  public File apply(
      String id, LidarRoofsAnalysisProcessor.RoofsAnalysisResult roofsAnalysisResults) {
    return apply(
        id, roofsAnalysisResults.toPointsExtractionResult(), Plane3DExtractorConf.getDefault());
  }

  @Override
  public File apply(String id, PointsExtractionResult result) {
    return apply(id, result, Plane3DExtractorConf.getDefault());
  }

  public File apply(String id, PointsExtractionResult result, Plane3DExtractorConf conf) {
    var buildingsData =
        result.data().values().stream().map(roof -> toBuildingData(roof, conf)).toList();

    try {
      var cityJsonFile = cityJsonFactory.make(id, id, buildingsData);
      log.info("CityJSON file saved at {}", cityJsonFile.getAbsolutePath());
      return cityJsonFile;
    } catch (CityJsonException e) {
      throw new RuntimeException(e);
    }
  }

  private BuildingData toBuildingData(DelimitedRoofPoints roof, Plane3DExtractorConf conf) {
    var roofProperty = new Building3DProperties(conf, null, roof, exporter);
    var planes = roofProperty.getRoofPlanes();
    var area2DScale = getArea2DScale(roof, planes);
    var distance2DScale = Math.sqrt(area2DScale);

    var groundZ =
        roofProperty.getCleanedGroundPoints().stream()
            .mapToDouble(LasPointGeometry::getZ)
            .average()
            .orElse(0);

    var roofs =
        planes.stream()
            .map(plane -> toPolygonWithProperties(plane, area2DScale, distance2DScale))
            .toList();

    var walls =
        planes.stream().map(plane -> createWalls(plane, groundZ)).toList().stream()
            .flatMap(List::stream)
            .toList();

    var grounds = planes.stream().map(plane -> createGround(plane, groundZ)).toList();

    return BuildingData.builder()
        .id(randomUUID().toString())
        .walls(walls)
        .roofs(roofs)
        .grounds(grounds)
        .properties(new HashMap<>())
        .build();
  }

  private static GeometryWithProperties toPolygonWithProperties(
      RoofPlane3D plane, double area2DScale, double distance2DScale) {
    var slope = plane.getSlopeInDegrees().getValue();
    var area2D = plane.get2DArea() * area2DScale;
    var area3D = Math.abs(round2(area2D / Math.cos(Math.toRadians(slope))));

    return GeometryWithProperties.builder()
        .geometry(plane.getDelimitation())
        .properties(
            Map.of(
                PLANE_SLOPE_KEY, slope,
                AREA_KEY, area3D,
                DISTANCE_2D_SCALE, distance2DScale))
        .build();
  }

  private static List<GeometryWithProperties> createWalls(RoofPlane3D plane, double groundZ) {
    var roofPolygon = plane.getDelimitation();
    return BuildingWallPolygonFactory.make(roofPolygon, groundZ);
  }

  private static GeometryWithProperties createGround(RoofPlane3D plane, double groundZ) {
    var roofPolygon = plane.getDelimitation();
    var groundPolygon = BuildingGroundPolygonFactory.make(roofPolygon, groundZ);
    return new GeometryWithProperties(groundPolygon, Map.of());
  }

  private static double getArea2DScale(DelimitedRoofPoints roof, List<RoofPlane3D> planes) {
    if (ROOF_SEGMENT_FACE_DELIMITATION.equals(roof.getType())) {
      return 1;
    }

    var delimitation2DArea = roof.getArea();
    var planes2DArea = planes.stream().mapToDouble(RoofPlane3D::get2DArea).sum();
    return delimitation2DArea / planes2DArea;
  }
}
