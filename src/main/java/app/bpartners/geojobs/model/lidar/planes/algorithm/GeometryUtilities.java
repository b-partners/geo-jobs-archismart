package app.bpartners.geojobs.model.lidar.planes.algorithm;

import app.bpartners.geojobs.model.lidar.LasPointGeometry;
import java.util.Collection;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Polygon;

public class GeometryUtilities {
  private GeometryUtilities() {}

  public static boolean isCompact(Polygon polygon, double epsilon) {
    double area = polygon.getArea();
    double perimeter = polygon.getLength();
    double compactness = 4 * Math.PI * area / (perimeter * perimeter);

    return compactness > epsilon;
  }

  public static Geometry intersection(Geometry a, Geometry b) {
    if (!a.isValid()) a = a.buffer(0);
    if (!b.isValid()) b = b.buffer(0);
    return a.intersection(b);
  }

  public static LasPointGeometry centroid(Collection<LasPointGeometry> points) {
    double sx = 0;
    double sy = 0;
    double sz = 0;
    for (var point : points) {
      sx += point.getX();
      sy += point.getY();
      sz += point.getZ();
    }

    int count = points.size();
    return new LasPointGeometry(sx / count, sy / count, sz / count);
  }
}
