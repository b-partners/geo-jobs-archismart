package app.bpartners.geojobs.model.lidar.planes.algorithm;

import app.bpartners.geojobs.model.lidar.LasPointGeometry;
import app.bpartners.geojobs.model.lidar.planes.Plane3D;
import java.util.Collection;
import java.util.List;

public class LasPointGeometryUtilities {
  private LasPointGeometryUtilities() {}

  public static List<LasPointGeometry> project(Collection<LasPointGeometry> points, Plane3D plane) {
    return points.stream()
        .map(
            point ->
                new LasPointGeometry(
                    point.getX(), point.getY(), plane.zAt(point.getX(), point.getY())))
        .toList();
  }
}
