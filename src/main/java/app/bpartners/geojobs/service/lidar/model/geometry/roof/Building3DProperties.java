package app.bpartners.geojobs.service.lidar.model.geometry.roof;

import static app.bpartners.geojobs.service.lidar.model.LidarDataStatus.*;

import app.bpartners.geojobs.model.lidar.LasPointGeometry;
import app.bpartners.geojobs.model.lidar.planes.Plane3DExtractorConf;
import app.bpartners.geojobs.model.lidar.planes.Planes3DExtractor;
import app.bpartners.geojobs.model.lidar.planes.exporter.Plane3DExtractionStepExporter;
import app.bpartners.geojobs.service.lidar.preprocessing.ground.GroundPointsCleaner;
import app.bpartners.geojobs.service.lidar.preprocessing.roof.RoofPointsCleaner;
import java.util.*;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.Polygon;

@Slf4j
@RequiredArgsConstructor
public class Building3DProperties {
  @Getter private final LidarRoofData data;
  @Getter private final Plane3DExtractorConf conf;
  @Getter private final Plane3DExtractionStepExporter exporter;

  public Building3DProperties(LidarRoofData data) {
    this(data, Plane3DExtractorConf.getDefault(), null);
  }

  // properties
  private List<RoofPlane3D> roofPlanes;
  private BuildingHeightInMeters buildingHeightInMeters;

  // cleaned data
  private Set<LasPointGeometry> cleanedRoofPoints;
  private Set<LasPointGeometry> cleanedGroundPoints;

  public BuildingHeightInMeters getHeightInMeters() {
    if (hasInvalidData()) {
      return new BuildingHeightInMeters(List.of(), List.of());
    }

    if (buildingHeightInMeters == null) {
      buildingHeightInMeters =
          new BuildingHeightInMeters(getCleanedRoofPoints(), getCleanedGroundPoints());
    }

    return buildingHeightInMeters;
  }

  public List<RoofPlane3D> getRoofPlanes() {
    if (hasInvalidData()) {
      return List.of();
    }

    if (roofPlanes != null) {
      return roofPlanes;
    }

    var extractor = new Planes3DExtractor(conf, exporter);
    var rawPlanes = extractor.apply(getCleanedRoofPoints());
    roofPlanes =
        rawPlanes.stream()
            .map(
                plane ->
                    new RoofPlane3D(
                        toPolygon(data.roof().boundaryLambert93()),
                        plane,
                        conf.planeDelimitationConf().concaveRatio(),
                        conf.planeDelimitationConf().simplificationEpsilon()))
            .toList();
    return roofPlanes;
  }

  public boolean hasInvalidData() {
    if (!AVAILABLE.equals(data.status())) {
      return true;
    }

    if (data.roof().points().size() < conf.planeConf().minPointsCount()) {
      return true;
    }

    return data.ground().points().size() < conf.planeConf().minPointsCount();
  }

  public Set<LasPointGeometry> getCleanedRoofPoints() {
    if (cleanedRoofPoints == null) {
      var cleaner = new RoofPointsCleaner(conf.roofPointsCleanerConf().duplicateXYTolerance());
      cleanedRoofPoints = cleaner.apply(data.roof().points());
    }
    return cleanedRoofPoints;
  }

  public Set<LasPointGeometry> getCleanedGroundPoints() {
    if (cleanedGroundPoints == null) {
      var cleaner = new GroundPointsCleaner();
      cleanedGroundPoints = cleaner.apply(data.ground().points());
    }
    return cleanedGroundPoints;
  }

  private static Polygon toPolygon(Geometry geometry) {
    return switch (geometry) {
      case Polygon polygon -> polygon;
      case MultiPolygon multiPolygon -> {
        if (1 != multiPolygon.getNumGeometries()) {
          log.warn("Unsupported polygon type");
        }
        yield (Polygon) multiPolygon.getGeometryN(0);
      }
      default -> throw new IllegalArgumentException("Unexpected type retrieved");
    };
  }
}
