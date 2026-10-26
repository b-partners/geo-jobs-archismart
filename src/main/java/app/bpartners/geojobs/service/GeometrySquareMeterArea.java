package app.bpartners.geojobs.service;

import app.bpartners.geojobs.service.lidar.api.SwissBoundaryChecker;
import app.bpartners.geojobs.service.lidar.api.WalloniaBoundaryChecker;
import java.util.function.Function;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.geometry.jts.JTS;
import org.geotools.referencing.CRS;
import org.locationtech.jts.geom.Geometry;
import org.springframework.stereotype.Component;

@Component
public class GeometrySquareMeterArea implements Function<Geometry, Double> {

  public static final CoordinateReferenceSystem WGS84;
  public static final CoordinateReferenceSystem LAMBERT_93;
  public static final CoordinateReferenceSystem EPSG_2056;
  public static final CoordinateReferenceSystem EPSG_3812;
  private static final SwissBoundaryChecker SWISS_BOUNDARY_CHECKER = new SwissBoundaryChecker();
  private static final WalloniaBoundaryChecker WALLONIA_BOUNDARY_CHECKER =
      new WalloniaBoundaryChecker();

  static {
    try {
      WGS84 = CRS.decode("EPSG:4326", true);
      LAMBERT_93 = CRS.decode("EPSG:2154", true);
      EPSG_2056 = CRS.decode("EPSG:2056", true);
      EPSG_3812 = CRS.decode("EPSG:3812", true);
    } catch (Exception e) {
      throw new RuntimeException("Erreur chargement des CRS", e);
    }
  }

  @Override
  public Double apply(Geometry geometryWGS84) {
    return project(geometryWGS84, WGS84, resolveProjectedCRS(geometryWGS84)).getArea();
  }

  private static CoordinateReferenceSystem resolveProjectedCRS(Geometry geometryWGS84) {
    if (SWISS_BOUNDARY_CHECKER.isGeometryInSwiss(geometryWGS84)) {
      return EPSG_2056;
    }
    if (WALLONIA_BOUNDARY_CHECKER.isGeometryInWallonia(geometryWGS84)) {
      return EPSG_3812;
    }
    return LAMBERT_93;
  }

  public Geometry project(
      Geometry geometry, CoordinateReferenceSystem source, CoordinateReferenceSystem target) {
    try {
      var transform = CRS.findMathTransform(source, target, true);
      return JTS.transform(geometry, transform);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
}
