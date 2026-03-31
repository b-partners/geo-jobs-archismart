package app.bpartners.geojobs.model.lidar.planes.topology.algorithm;

import app.bpartners.geojobs.model.lidar.LasPointGeometry;
import app.bpartners.geojobs.model.lidar.planes.Plane3D;
import app.bpartners.geojobs.model.lidar.planes.algorithm.Vector3DUtils;
import app.bpartners.geojobs.model.lidar.planes.topology.model.Line3D;
import java.util.Optional;
import org.locationtech.jts.math.Vector3D;

public class Plane3DIntersection {
  private Plane3DIntersection() {}

  private static final double PARALLEL_EPSILON = 1e-8;

  public static Optional<Line3D> intersects(Plane3D a, Plane3D b) {
    var n1 = Vector3DUtils.from(a);
    var n2 = Vector3DUtils.from(b);

    var direction = Vector3DUtils.cross(n1, n2);
    if (direction.length() < PARALLEL_EPSILON) {
      return Optional.empty();
    }

    var optionalPoint = resolve(direction, n1, n2, a.getD(), b.getD());
    return optionalPoint.map(point -> new Line3D(point, direction));
  }

  private static Optional<LasPointGeometry> resolve(
      Vector3D direction, Vector3D n1, Vector3D n2, double d1, double d2) {
    double absX = Math.abs(direction.getX());
    double absY = Math.abs(direction.getY());
    double absZ = Math.abs(direction.getZ());

    if (absX >= absY && absX >= absZ) {
      return resolveWithX0(n1, n2, d1, d2);
    } else if (absY >= absX && absY >= absZ) {
      return resolveWithY0(n1, n2, d1, d2);
    } else {
      return resolveWithZ0(n1, n2, d1, d2);
    }
  }

  private static Optional<LasPointGeometry> resolveWithX0(
      Vector3D n1, Vector3D n2, double d1, double d2) {
    double[][] a = {{n1.getY(), n1.getZ()}, {n2.getY(), n2.getZ()}};
    double[] b = {-d1, -d2};
    return solve2x2(a, b).map(sol -> new LasPointGeometry(0, sol[0], sol[1]));
  }

  private static Optional<LasPointGeometry> resolveWithZ0(
      Vector3D n1, Vector3D n2, double d1, double d2) {
    double[][] a = {{n1.getX(), n1.getY()}, {n2.getX(), n2.getY()}};
    double[] b = {-d1, -d2};
    return solve2x2(a, b).map(sol -> new LasPointGeometry(sol[0], sol[1], 0));
  }

  private static Optional<LasPointGeometry> resolveWithY0(
      Vector3D n1, Vector3D n2, double d1, double d2) {
    double[][] a = {{n1.getX(), n1.getZ()}, {n2.getX(), n2.getZ()}};
    double[] b = {-d1, -d2};
    return solve2x2(a, b).map(sol -> new LasPointGeometry(sol[0], 0, sol[1]));
  }

  private static Optional<double[]> solve2x2(double[][] a, double[] b) {
    double det = a[0][0] * a[1][1] - a[0][1] * a[1][0];

    if (Math.abs(det) < PARALLEL_EPSILON) {
      return Optional.empty();
    }

    double u = (b[0] * a[1][1] - b[1] * a[0][1]) / det;
    double v = (a[0][0] * b[1] - a[1][0] * b[0]) / det;
    return Optional.of(new double[] {u, v});
  }
}
