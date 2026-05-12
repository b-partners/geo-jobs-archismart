package app.bpartners.geojobs.model.lidar.planes.postprocessing;

import static app.bpartners.geojobs.model.geometry.GeometryFactory.geometryFactory;
import static app.bpartners.geojobs.model.lidar.planes.postprocessing.Snapping2DComputer.extractAllPoints;

import app.bpartners.geojobs.model.lidar.planes.Plane3D;
import java.util.*;
import java.util.function.UnaryOperator;
import lombok.RequiredArgsConstructor;
import org.locationtech.jts.geom.Coordinate;

@RequiredArgsConstructor
public class Snapping3DComputer implements UnaryOperator<List<Plane3D>> {
  private final double threshold;

  @Override
  public List<Plane3D> apply(List<Plane3D> planes) {
    var allPoints = extractAllPoints(planes);
    var clusters = buildClusters3D(allPoints);
    var centroids = computeCentroids3D(clusters);

    return rebuildPlanes(planes, centroids);
  }

  private Map<Long, Cluster3D> buildClusters3D(List<Coordinate> points) {
    Map<Long, Cluster3D> clusters = new HashMap<>();

    for (var c : points) {
      long key = createKey(c);
      clusters.computeIfAbsent(key, k -> new Cluster3D()).add(c);
    }

    return clusters;
  }

  private Map<Long, Coordinate> computeCentroids3D(Map<Long, Cluster3D> clusters) {
    Map<Long, Coordinate> centroids = new HashMap<>();

    for (var e : clusters.entrySet()) {
      centroids.put(e.getKey(), e.getValue().centroid());
    }

    return centroids;
  }

  private List<Plane3D> rebuildPlanes(List<Plane3D> planes, Map<Long, Coordinate> centroids) {
    List<Plane3D> result = new ArrayList<>();

    for (var plane : planes) {
      var coordinates = plane.getDelimitation().getCoordinates();
      var snapped = new Coordinate[coordinates.length];

      for (int i = 0; i < coordinates.length; i++) {
        var c = centroids.get(createKey(coordinates[i]));
        snapped[i] = new Coordinate(c.x, c.y, c.z);
      }

      var polygon = geometryFactory.createPolygon(snapped);
      result.add(
          plane.toBuilder()
              .points(plane.getPoints())
              .delimitation(polygon)
              .slopeInDegrees(plane.getSlopeInDegrees())
              .build());
    }

    return result;
  }

  private long createKey(Coordinate c) {
    long x = (long) Math.floor(c.x / threshold);
    long y = (long) Math.floor(c.y / threshold);
    long z = (long) Math.floor(c.z / threshold);

    return (x << 40) ^ (y << 20) ^ z;
  }

  private static class Cluster3D {
    double sumX;
    double sumY;
    double sumZ;
    int count;

    void add(Coordinate c) {
      sumX += c.x;
      sumY += c.y;
      sumZ += c.z;
      count++;
    }

    Coordinate centroid() {
      return new Coordinate(sumX / count, sumY / count, sumZ / count);
    }
  }
}
