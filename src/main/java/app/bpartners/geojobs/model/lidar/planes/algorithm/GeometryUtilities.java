package app.bpartners.geojobs.model.lidar.planes.algorithm;

import static app.bpartners.geojobs.model.geometry.GeometryFactory.geometryFactory;

import app.bpartners.geojobs.model.lidar.LasPointGeometry;
import app.bpartners.geojobs.model.lidar.planes.Plane3D;
import java.util.Arrays;
import java.util.Collection;
import java.util.function.Function;
import org.locationtech.jts.geom.*;

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

  public static Coordinate[] zSetter(Coordinate[] coordinates, ZSetterCallback callback) {
    return Arrays.stream(coordinates)
        .map(
            coordinate -> {
              var x = coordinate.getX();
              var y = coordinate.getY();
              var z = callback.apply(coordinate);
              return new Coordinate(x, y, z);
            })
        .toArray(Coordinate[]::new);
  }

  public static Polygon zSetter(Polygon polygon, ZSetterCallback callback) {
    var projectedCoordinates = zSetter(polygon.getCoordinates(), callback);

    if (!projectedCoordinates[0].equals2D(projectedCoordinates[projectedCoordinates.length - 1])) {
      projectedCoordinates = Arrays.copyOf(projectedCoordinates, projectedCoordinates.length + 1);
      projectedCoordinates[projectedCoordinates.length - 1] = projectedCoordinates[0];
    }

    return geometryFactory.createPolygon(projectedCoordinates);
  }

  public static Polygon project(Plane3D plane, Polygon polygon) {
    return zSetter(polygon, coordinate -> plane.zAt(coordinate.getX(), coordinate.getY()));
  }

  public static Coordinate[] project(Plane3D plane, Coordinate[] coordinates) {
    return zSetter(coordinates, coordinate -> plane.zAt(coordinate.getX(), coordinate.getY()));
  }

  public interface ZSetterCallback extends Function<Coordinate, Double> {}

  public static Polygon getLargestPolygon(Geometry geometry) {
    if (geometry instanceof Polygon polygon) {
      return polygon;
    }

    if (geometry instanceof MultiPolygon multiPolygon) {
      Polygon largest = null;
      double maxArea = -1.0;

      for (int i = 0; i < multiPolygon.getNumGeometries(); i++) {
        var current = (Polygon) multiPolygon.getGeometryN(i);
        double currentArea = current.getArea();

        if (currentArea > maxArea) {
          maxArea = currentArea;
          largest = current;
        }
      }
      return largest;
    }

    throw new IllegalArgumentException("Unsupported geometry type: " + geometry.getGeometryType());
  }

  public static LineString extend(LineString line, double delta) {
    var coordinates = line.getCoordinates();
    var start = coordinates[0];
    var end = coordinates[coordinates.length - 1];

    double dx = end.getX() - start.getX();
    double dy = end.getY() - start.getY();
    double length = Math.hypot(dx, dy);

    double ux = dx / length;
    double uy = dy / length;

    var newStart =
        new Coordinate(start.getX() - ux * delta, start.getY() - uy * delta, start.getZ());

    var newEnd = new Coordinate(end.getX() + ux * delta, end.getY() + uy * delta, end.getZ());

    return geometryFactory.createLineString(new Coordinate[] {newStart, newEnd});
  }
}
