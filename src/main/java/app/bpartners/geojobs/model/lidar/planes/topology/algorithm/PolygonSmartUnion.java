package app.bpartners.geojobs.model.lidar.planes.topology.algorithm;

import java.util.Optional;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Polygon;

public class PolygonSmartUnion {
  private PolygonSmartUnion() {}

  public static Optional<Polygon> union(
      Polygon a, Polygon b, double maxDistance, double minIntersectionDistance) {
    if (a.distance(b) > maxDistance) {
      return Optional.empty();
    }

    var halfMaxDistance = maxDistance / 2;
    var aBuffered = a.buffer(halfMaxDistance);
    var bBuffered = b.buffer(halfMaxDistance);
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
    return Optional.of((Polygon) union);
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
}
