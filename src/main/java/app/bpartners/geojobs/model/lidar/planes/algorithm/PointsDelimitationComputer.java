package app.bpartners.geojobs.model.lidar.planes.algorithm;

import static app.bpartners.geojobs.model.geometry.GeometryFactory.geometryFactory;
import static org.locationtech.jts.algorithm.hull.ConcaveHull.concaveHullByLengthRatio;

import app.bpartners.geojobs.model.lidar.LasPointGeometry;
import java.util.Collection;
import lombok.extern.slf4j.Slf4j;
import org.locationtech.jts.algorithm.ConvexHull;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.Polygon;

@Slf4j
public class PointsDelimitationComputer {
  private PointsDelimitationComputer() {}

  public static Polygon getConcave(Collection<LasPointGeometry> points, double ratio) {
    var multiPoint = geometryFactory.createMultiPointFromCoords(getCoordinates(points));

    var hull = concaveHullByLengthRatio(multiPoint, ratio);

    if (!hull.isValid()) {
      log.warn("Concave hull produced an invalid geometry. Attempting to fix it with buffer(0).");
      hull = hull.buffer(0);
    }

    return toPolygon(hull);
  }

  public static Polygon getConvex(Collection<LasPointGeometry> points) {
    var hull = new ConvexHull(getCoordinates(points), geometryFactory).getConvexHull();
    return toPolygon(hull);
  }

  private static Polygon toPolygon(Geometry geometry) {
    return switch (geometry) {
      case Polygon polygon -> polygon;
      case MultiPolygon multiPolygon -> (Polygon) multiPolygon.getGeometryN(0);
      default -> throw new IllegalArgumentException("Invalid points delimitation retrieved");
    };
  }

  private static Coordinate[] getCoordinates(Collection<LasPointGeometry> points) {
    return points.stream().map(LasPointGeometry::getCoordinate).toArray(Coordinate[]::new);
  }
}
