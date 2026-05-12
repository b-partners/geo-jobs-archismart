package app.bpartners.geojobs.service.lidar.model.geometry.roof;

import static app.bpartners.geojobs.model.lidar.planes.algorithm.GeometryUtilities.getLargestPolygon;

import app.bpartners.geojobs.model.lidar.LasPointGeometry;
import app.bpartners.geojobs.model.lidar.planes.Plane3D;
import app.bpartners.geojobs.model.lidar.planes.Plane3DGeneratorWithoutSegmentations;
import app.bpartners.geojobs.model.lidar.planes.Planes3DExtractor;
import app.bpartners.geojobs.model.lidar.planes.conf.Plane3DExtractorConf;
import app.bpartners.geojobs.model.lidar.planes.exporter.Plane3DExtractionStepExporter;
import app.bpartners.geojobs.model.lidar.planes.model.DelimitedRoofPoints;
import app.bpartners.geojobs.service.lidar.preprocessing.ground.GroundPointsCleaner;
import app.bpartners.geojobs.service.lidar.preprocessing.roof.RoofPointsCleaner;
import java.util.*;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.locationtech.jts.geom.Polygon;

@Slf4j
@RequiredArgsConstructor
public class Building3DProperties {
  @Getter private final Plane3DExtractorConf conf;
  @Deprecated @Getter private final LidarRoofData data;
  @Getter private final DelimitedRoofPoints delimitedPoints;
  @Getter private final Plane3DExtractionStepExporter exporter;

  @Deprecated
  public Building3DProperties(LidarRoofData data) {
    this(Plane3DExtractorConf.getDefault(), data, data.toDelimitedRoofPoints(), null);
  }

  public Building3DProperties(DelimitedRoofPoints delimitedPoints) {
    this(Plane3DExtractorConf.getDefault(), null, delimitedPoints, null);
  }

  // properties
  private List<RoofPlane3D> roofPlanes;
  private BuildingHeightInMeters buildingHeightInMeters;

  // cleaned data
  private Set<LasPointGeometry> cleanedRoofPoints;
  private Set<LasPointGeometry> cleanedGroundPoints;

  public BuildingHeightInMeters getHeightInMeters() {
    if (buildingHeightInMeters == null) {
      buildingHeightInMeters =
          new BuildingHeightInMeters(getCleanedRoofPoints(), getCleanedGroundPoints());
    }

    return buildingHeightInMeters;
  }

  public List<RoofPlane3D> getRoofPlanes() {
    if (roofPlanes != null) {
      return roofPlanes;
    }

    var rawPlanes = getRawPlanes();
    roofPlanes =
        rawPlanes.stream().map(plane -> new RoofPlane3D(getRoofDelimitation(), plane)).toList();
    return roofPlanes;
  }

  public Set<LasPointGeometry> getCleanedRoofPoints() {
    if (cleanedRoofPoints == null) {
      var cleaner = new RoofPointsCleaner(conf.roofPointsCleanerConf().duplicateXYTolerance());
      cleanedRoofPoints = cleaner.apply(delimitedPoints.getPoints());
    }
    return cleanedRoofPoints;
  }

  public Set<LasPointGeometry> getCleanedGroundPoints() {
    if (cleanedGroundPoints == null) {
      var cleaner = new GroundPointsCleaner();
      cleanedGroundPoints = cleaner.apply(delimitedPoints.getGroundPoints());
    }
    return cleanedGroundPoints;
  }

  @Deprecated
  private Polygon getRoofDelimitation() {
    return getLargestPolygon(delimitedPoints);
  }

  private List<Plane3D> getRawPlanes() {
    return switch (delimitedPoints.getType()) {
      case ENTIRE_ROOF_DELIMITATION -> {
        var extractor = new Planes3DExtractor(getRoofDelimitation(), conf, exporter);
        yield extractor.apply(delimitedPoints.getPoints());
      }
      case ROOF_SEGMENT_FACE_DELIMITATION -> {
        var extractor = new Plane3DGeneratorWithoutSegmentations(conf);
        yield extractor.apply(delimitedPoints);
      }
    };
  }
}
