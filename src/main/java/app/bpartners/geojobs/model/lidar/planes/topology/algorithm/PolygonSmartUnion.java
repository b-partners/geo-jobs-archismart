package app.bpartners.geojobs.model.lidar.planes.topology.algorithm;

import app.bpartners.geojobs.model.lidar.planes.algorithm.OBB2DComputer;
import java.util.Arrays;
import java.util.Optional;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Polygon;

public class PolygonSmartUnion {
  private PolygonSmartUnion() {}

  private static final double LENGTH_TEST = 0.5;
  private static final double MAX_Z_DISTANCE = 0.2;

  public static Optional<Polygon> union(
      Polygon a, Polygon b, double maxDistance, double minIntersectionDistance) {
    if (a.distance(b) > maxDistance) {
      return Optional.empty();
    }

    if (!shouldMerge(a, b)) {
      return Optional.empty();
    }

    var aBuffered = a.buffer(maxDistance);
    var bBuffered = b.buffer(maxDistance);
    if (!aBuffered.intersects(bBuffered)) {
      return Optional.empty();
    }

    var intersection = aBuffered.intersection(bBuffered).buffer(0);
    if (intersection.isEmpty() || getMaxSize(intersection.buffer(0)) < minIntersectionDistance) {
      return Optional.empty();
    }

    intersection = intersection.buffer(0);
    if (isNotAPolygon(intersection)) {
      return Optional.empty();
    }

    var bufferPart = intersection.difference(a.union(b).buffer(0)).buffer(0.3);
    var union = a.union(bufferPart).buffer(0).union(b).buffer(0);
    if (isNotAPolygon(union)) {
      return Optional.empty();
    }

    var unionPolygon = (Polygon) union;
    if (isValidUnion(a, b, unionPolygon)) {
      return Optional.of(unionPolygon);
    }

    return Optional.empty();
  }

  private static boolean isNotAPolygon(Geometry geometry) {
    return !(geometry instanceof Polygon);
  }

  private static double getMaxSize(Geometry geometry) {
    var envelope = geometry.getEnvelopeInternal();
    double width = envelope.getWidth();
    double height = envelope.getHeight();
    return Math.max(width, height);
  }

  private static boolean shouldMerge(Polygon a, Polygon b) {
    double minZa = getMinZ(a);
    double maxZa = getMaxZ(a);
    double minZb = getMinZ(b);
    double maxZb = getMaxZ(b);

    return minZa <= maxZb + MAX_Z_DISTANCE && minZb <= maxZa + MAX_Z_DISTANCE;
  }

  private static boolean isValidUnion(Polygon a, Polygon b, Polygon union) {
    var obbComputer = new OBB2DComputer();

    var aOBB = obbComputer.apply(a);
    var bOBB = obbComputer.apply(b);
    var uOBB = obbComputer.apply(union);

    double aMax = Math.max(aOBB.width(), aOBB.height());
    double aMin = Math.min(aOBB.width(), aOBB.height());
    double bMax = Math.max(bOBB.width(), bOBB.height());
    double bMin = Math.min(bOBB.width(), bOBB.height());
    double uMax = Math.max(uOBB.width(), uOBB.height());
    double uMin = Math.min(uOBB.width(), uOBB.height());

    boolean growsInLength = uMax > (Math.max(aMax, bMax) + Math.min(aMax, bMax) * LENGTH_TEST);
    boolean growsInWidth = uMin > (Math.max(aMin, bMin) + Math.min(aMin, bMin) * LENGTH_TEST);
    return !(growsInLength && growsInWidth);
  }

  private static double getMinZ(Polygon p) {
    return Arrays.stream(p.getCoordinates()).mapToDouble(Coordinate::getZ).min().orElse(0);
  }

  private static double getMaxZ(Polygon p) {
    return Arrays.stream(p.getCoordinates()).mapToDouble(Coordinate::getZ).max().orElse(0);
  }
}
