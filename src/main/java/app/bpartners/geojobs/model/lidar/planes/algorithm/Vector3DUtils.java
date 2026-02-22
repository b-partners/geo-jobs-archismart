package app.bpartners.geojobs.model.lidar.planes.algorithm;

import app.bpartners.geojobs.model.lidar.planes.Plane3D;
import org.locationtech.jts.math.Vector3D;

public class Vector3DUtils {
  private Vector3DUtils() {}

  public static double getAngleInDegrees(Vector3D v1, Vector3D v2) {
    var n1 = v1.normalize();
    var n2 = v2.normalize();
    var dot = Math.clamp(n1.dot(n2), -1.0, 1.0);
    var angleRad = Math.acos(dot);
    return Math.toDegrees(angleRad);
  }

  public static boolean hasSameDirectionIgnoringOrientation(
      Vector3D a, Vector3D b, double degEpsilon) {
    double angle = getAngleInDegrees(a, b);
    return angle <= degEpsilon || Math.abs(angle - 180.0) <= degEpsilon;
  }

  public static boolean hasPerpendicularDirection(Vector3D a, Vector3D b, double degEpsilon) {
    double angle = getAngleInDegrees(a, b);
    return Math.abs(angle - 90.0) <= degEpsilon;
  }

  public static Vector3D from(Plane3D plane) {
    return new Vector3D(plane.getA(), plane.getB(), plane.getC());
  }
}
