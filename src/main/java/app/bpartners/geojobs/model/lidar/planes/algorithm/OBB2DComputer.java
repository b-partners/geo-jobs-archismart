package app.bpartners.geojobs.model.lidar.planes.algorithm;

import static app.bpartners.geojobs.service.lidar.model.LidarClass.BATIMENT;
import static java.lang.Double.NEGATIVE_INFINITY;
import static java.lang.Double.POSITIVE_INFINITY;

import app.bpartners.geojobs.model.lidar.LasPointGeometry;
import app.bpartners.geojobs.model.lidar.planes.Plane3D;
import app.bpartners.geojobs.model.lidar.planes.algorithm.model.OBB2D;
import java.util.function.Function;
import org.locationtech.jts.geom.Polygon;

public class OBB2DComputer implements Function<Polygon, OBB2D> {
  @Override
  public OBB2D apply(Polygon polygon) {
    var best = OBB2D.builder().area(POSITIVE_INFINITY).build();
    var coordinates = polygon.getCoordinates();

    var n = coordinates.length - 1;
    for (int i = 0; i < n; i++) {
      var p0 = coordinates[i];
      var p1 = coordinates[(i + 1) % n];

      double dx = p1.getX() - p0.getX();
      double dy = p1.getY() - p0.getY();

      double angle = Math.atan2(dy, dx);

      double cos = Math.cos(-angle);
      double sin = Math.sin(-angle);

      double minX = POSITIVE_INFINITY;
      double maxX = NEGATIVE_INFINITY;
      double minY = POSITIVE_INFINITY;
      double maxY = NEGATIVE_INFINITY;

      for (var p : coordinates) {
        double x = p.getX() * cos - p.getY() * sin;
        double y = p.getX() * sin + p.getY() * cos;

        minX = Math.min(minX, x);
        maxX = Math.max(maxX, x);
        minY = Math.min(minY, y);
        maxY = Math.max(maxY, y);
      }

      double width = maxX - minX;
      double height = maxY - minY;
      double area = width * height;

      if (area > best.area()) {
        continue;
      }
      double cx = (minX + maxX) * 0.5;
      double cy = (minY + maxY) * 0.5;

      double rcx = cx * Math.cos(angle) - cy * Math.sin(angle);
      double rcy = cx * Math.sin(angle) + cy * Math.cos(angle);

      best =
          OBB2D
              .builder()
              .area(area)
              .angle(angle)
              .width(width)
              .height(height)
              .center(new LasPointGeometry(rcx, rcy, 0, BATIMENT))
              .build();
    }
    return best;
  }

  public OBB2D apply(Plane3D plane) {
    return apply(plane.getConvexDelimitation());
  }
}
