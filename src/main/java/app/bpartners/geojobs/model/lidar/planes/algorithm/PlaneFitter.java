package app.bpartners.geojobs.model.lidar.planes.algorithm;

import static app.bpartners.geojobs.model.lidar.planes.algorithm.GeometryUtilities.centroid;

import app.bpartners.geojobs.model.lidar.LasPointGeometry;
import app.bpartners.geojobs.model.lidar.planes.Kernel;
import app.bpartners.geojobs.model.lidar.planes.Plane3D;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import org.locationtech.jts.math.Vector3D;

public class PlaneFitter {
  private PlaneFitter() {}

  public static Plane3D fit(Collection<LasPointGeometry> points) {
    var centroid = centroid(points);

    double xx = 0;
    double xy = 0;
    double xz = 0;
    double yy = 0;
    double yz = 0;
    double zz = 0;
    for (var p : points) {
      double dx = p.getX() - centroid.getX();
      double dy = p.getY() - centroid.getY();
      double dz = p.getZ() - centroid.getZ();

      xx += dx * dx;
      xy += dx * dy;
      xz += dx * dz;
      yy += dy * dy;
      yz += dy * dz;
      zz += dz * dz;
    }

    var r0 = new Vector3D(xx, xy, xz);
    var r1 = new Vector3D(xy, yy, yz);
    var r2 = new Vector3D(xz, yz, zz);

    var v0 = Vector3DUtils.cross(r0, r1);
    var v1 = Vector3DUtils.cross(r0, r2);
    var v2 = Vector3DUtils.cross(r1, r2);

    double d0 = v0.dot(v0);
    double d1 = v1.dot(v1);
    double d2 = v2.dot(v2);

    Vector3D normal;
    if (d0 >= d1 && d0 >= d2) normal = v0;
    else if (d1 >= d0 && d1 >= d2) normal = v1;
    else normal = v2;

    normal = normal.normalize();
    if (normal.getZ() < 0) {
      normal = Vector3DUtils.negate(normal);
    }

    return toPlane(normal, centroid, points);
  }

  private static Plane3D toPlane(
      Vector3D normal, LasPointGeometry centroid, Collection<LasPointGeometry> points) {
    double a = normal.getX();
    double b = normal.getY();
    double c = normal.getZ();
    double d = -(a * centroid.getX() + b * centroid.getY() + c * centroid.getZ());
    return Plane3D.builder().a(a).b(b).c(c).d(d).points(new HashSet<>(points)).build();
  }

  public static Plane3D fit(Kernel kernel) {
    var chains = kernel.getChains();
    var triplet = chains.getOrthogonalTriplet();
    var p1 = triplet.getFirst();
    var p2 = triplet.get(1);
    var p3 = triplet.getLast();
    return fit(Set.of(p1, p2, p3)).toBuilder().kernel(kernel).build();
  }
}
