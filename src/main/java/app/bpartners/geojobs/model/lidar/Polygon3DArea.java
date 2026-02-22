package app.bpartners.geojobs.model.lidar;

import lombok.RequiredArgsConstructor;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Polygon;

@RequiredArgsConstructor
public class Polygon3DArea {
  private Double value;
  private final Polygon polygon;

  public double getValue() {
    if (value == null) {
      value = compute3DPolygonArea(polygon);
    }

    return value;
  }

  public static double compute3DPolygonArea(Polygon polygon) {
    var coordinates = polygon.getCoordinates();
    if (coordinates.length < 3) return 0.0;

    double totalArea = 0.0;

    var p0 = coordinates[0];
    for (int i = 1; i < coordinates.length - 2; i++) {
      var p1 = coordinates[i];
      var p2 = coordinates[i + 1];
      totalArea += triangle3DArea(p0, p1, p2);
    }

    return totalArea;
  }

  private static double triangle3DArea(Coordinate a, Coordinate b, Coordinate c) {
    double abx = b.x - a.x;
    double aby = b.y - a.y;
    double abz = b.z - a.z;

    double acx = c.x - a.x;
    double acy = c.y - a.y;
    double acz = c.z - a.z;

    double cx = aby * acz - abz * acy;
    double cy = abz * acx - abx * acz;
    double cz = abx * acy - aby * acx;

    return 0.5 * Math.sqrt(cx * cx + cy * cy + cz * cz);
  }
}
