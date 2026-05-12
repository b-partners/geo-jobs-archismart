package app.bpartners.geojobs.model.lidar.planes.postprocessing;

import static app.bpartners.geojobs.model.geometry.GeometryFactory.geometryFactory;
import static app.bpartners.geojobs.model.lidar.planes.algorithm.GeometryUtilities.project;

import app.bpartners.geojobs.model.lidar.planes.Plane3D;
import java.util.*;
import java.util.function.UnaryOperator;
import lombok.RequiredArgsConstructor;
import org.locationtech.jts.geom.Coordinate;

@RequiredArgsConstructor
public class Snapping2DComputer implements UnaryOperator<List<Plane3D>> {
  private final double threshold;

  @Override
  public List<Plane3D> apply(List<Plane3D> planes) {
    List<Coordinate> allPoints = extractAllPoints(planes);
    Map<Long, Cluster> clusters = buildClusters(allPoints);
    Map<Long, Coordinate> centroids = computeCentroids(clusters);

    return rebuildPlanes(planes, centroids);
  }

  static List<Coordinate> extractAllPoints(List<Plane3D> planes) {
    List<Coordinate> allPoints = new ArrayList<>();
    for (var p : planes) {
      allPoints.addAll(Arrays.asList(p.getDelimitation().getCoordinates()));
    }
    return allPoints;
  }

  private Map<Long, Cluster> buildClusters(List<Coordinate> allPoints) {
    Map<Long, Cluster> clusters = new HashMap<>();
    for (var c : allPoints) {
      long key = createKey(c);
      clusters.computeIfAbsent(key, k -> new Cluster()).add(c);
    }
    return clusters;
  }

  private Map<Long, Coordinate> computeCentroids(Map<Long, Cluster> clusters) {
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
        long key = createKey(coordinates[i]);
        var centroid = centroids.get(key);
        snapped[i] = new Coordinate(centroid.x, centroid.y, 1.0);
      }

      var polygon = geometryFactory.createPolygon(snapped);
      var newDelimitation = project(plane, polygon);

      result.add(plane.toBuilder().delimitation(newDelimitation).build());
    }

    return result;
  }

  private long createKey(Coordinate c) {
    long x = (long) Math.floor(c.x / threshold);
    long y = (long) Math.floor(c.y / threshold);
    return (x << 32) ^ y;
  }

  private static class Cluster {
    double sumX;
    double sumY;
    int count;

    void add(Coordinate c) {
      sumX += c.x;
      sumY += c.y;
      count++;
    }

    Coordinate centroid() {
      return new Coordinate(sumX / count, sumY / count);
    }
  }
}
