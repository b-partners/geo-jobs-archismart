package app.bpartners.geojobs.model.lidar.planes.topology.algorithm;

import static app.bpartners.geojobs.model.geometry.GeometryFactory.geometryFactory;
import static app.bpartners.geojobs.model.lidar.planes.algorithm.GeometryUtilities.getLargestPolygon;
import static app.bpartners.geojobs.model.lidar.planes.algorithm.GeometryUtilities.project;

import app.bpartners.geojobs.model.lidar.LasPointGeometry;
import app.bpartners.geojobs.model.lidar.planes.Plane3D;
import app.bpartners.geojobs.model.lidar.planes.algorithm.GeometryUtilities;
import app.bpartners.geojobs.model.lidar.planes.algorithm.PlaneFitter;
import java.util.*;
import org.locationtech.jts.densify.Densifier;
import org.locationtech.jts.geom.*;
import org.locationtech.jts.linearref.LengthIndexedLine;
import org.locationtech.jts.simplify.DouglasPeuckerSimplifier;

public class LineAndPolygonSnappingComputer {
  private LineAndPolygonSnappingComputer() {}

  private static final double SIMPLIFICATION = 0.1;
  private static final double MAX_DISTANCE = 3.0;
  private static final double DENSIFIED_DISTANCE = 0.5;

  public static Plane3D snap(Plane3D plane, LineString line) {
    var initial = plane.getDelimitation();
    var densified = Densifier.densify(initial, DENSIFIED_DISTANCE);
    var densifiedPolygon = getLargestPolygon(densified);
    densifiedPolygon = reorderCoordinates(densifiedPolygon, line);

    var maxProjection = getMaxProjected(densifiedPolygon, line);
    var newDelimitation = replaceClosedLine(maxProjection, densifiedPolygon, line);
    if (!newDelimitation.isValid()) {
      newDelimitation = getLargestPolygon(newDelimitation.buffer(0));
    }

    var newPlane = refit(plane.getDelimitation().getCoordinates(), plane, line);
    newDelimitation = project(newPlane, newDelimitation);

    return newPlane.toBuilder()
        .area(null)
        .delimitation(newDelimitation)
        .convexDelimitation(null)
        .build();
  }

  private static Plane3D refit(Coordinate[] coordinates, Plane3D plane, LineString line) {
    var centroid = centroid(coordinates);
    var points =
        new ArrayList<>(
            List.of(
                centroid,
                new LasPointGeometry(coordinates[0]),
                new LasPointGeometry(coordinates[coordinates.length - 2])));

    points.addAll(toPoints(line.getCoordinates()));

    var refittedPlane = PlaneFitter.fit(points);
    return plane.toBuilder()
        .a(refittedPlane.getA())
        .b(refittedPlane.getB())
        .c(refittedPlane.getC())
        .d(refittedPlane.getD())
        .norm(null)
        .build();
  }

  private static Polygon reorderCoordinates(Polygon polygon, LineString line) {
    var original = polygon.getExteriorRing().getCoordinates();
    int n = original.length - 1;

    int farthestIndex = 0;
    double maxDistance = -1.0;

    for (int i = 0; i < n; i++) {
      double currentDistance = line.distance(new LasPointGeometry(original[i]));
      if (currentDistance > maxDistance) {
        maxDistance = currentDistance;
        farthestIndex = i;
      }
    }

    var reordered = new Coordinate[original.length];
    for (int i = 0; i < n; i++) {
      reordered[i] = original[(farthestIndex + i) % n];
    }

    reordered[n] = reordered[0];
    return geometryFactory.createPolygon(reordered);
  }

  private static TwoIndexes getMaxProjected(Polygon polygon, LineString line) {
    var indexedLine = new LengthIndexedLine(line);
    var lineMin = indexedLine.getStartIndex();
    var lineMax = indexedLine.getEndIndex();

    var min = Double.POSITIVE_INFINITY;
    var max = Double.NEGATIVE_INFINITY;
    var minIndex = -1;
    var maxIndex = -1;

    var coordinates = polygon.getCoordinates();
    for (int i = 0; i < coordinates.length - 1; i++) {
      var coordinate = coordinates[i];
      var projection = indexedLine.project(coordinate);
      if (lineMax == projection || lineMin == projection) {
        continue;
      }

      var point = new LasPointGeometry(coordinate);
      if (line.distance(point) > MAX_DISTANCE) {
        continue;
      }

      if (min > projection) {
        minIndex = i;
        min = projection;
      }

      if (max < projection) {
        maxIndex = i;
        max = projection;
      }
    }

    return new TwoIndexes(minIndex, maxIndex);
  }

  private static Polygon replaceClosedLine(TwoIndexes indexes, Polygon polygon, LineString line) {
    if (indexes.b() == -1 || indexes.a() == -1) return polygon;

    var twoLines = splitIntoLines(polygon, indexes);
    var path1 = twoLines.getFirst();
    var path2 = twoLines.getLast();

    var distance1 = avgDistance(path1, line);
    var distance2 = avgDistance(path2, line);
    var toKeep = distance1 > distance2 ? path1 : path2;

    var finalCoordinates = new ArrayList<>(cleanAndSimplify(toKeep));
    var lineCoordinates = new ArrayList<>(Arrays.asList(line.getCoordinates()));

    var lastOfPath = finalCoordinates.getLast();
    var startOfLine = lineCoordinates.getFirst();
    var endOfLine = lineCoordinates.getLast();

    if (startOfLine.distance(lastOfPath) > endOfLine.distance(lastOfPath)) {
      Collections.reverse(lineCoordinates);
    }

    for (var coordinate : lineCoordinates) {
      var size = finalCoordinates.size();
      var last = finalCoordinates.getLast();
      var first = finalCoordinates.getFirst();

      if (last.distance(coordinate) < DENSIFIED_DISTANCE) {
        finalCoordinates.set(size - 1, coordinate);
      } else if (first.distance(coordinate) < DENSIFIED_DISTANCE) {
        finalCoordinates.set(0, coordinate);
      } else {
        finalCoordinates.add(coordinate);
      }
    }

    var first = finalCoordinates.getFirst();
    var last = finalCoordinates.getLast();

    if (!first.equals2D(last)) {
      finalCoordinates.add(new Coordinate(first));
    }
    return geometryFactory.createPolygon(finalCoordinates.toArray(Coordinate[]::new));
  }

  private static List<List<Coordinate>> splitIntoLines(Polygon polygon, TwoIndexes indexes) {
    var coordinates = polygon.getCoordinates();
    var i1 = Math.min(indexes.a(), indexes.b());
    var i2 = Math.max(indexes.a(), indexes.b());

    var path1 = new ArrayList<>(Arrays.asList(coordinates).subList(i1, i2 + 1));
    var path2 = new ArrayList<>(Arrays.asList(coordinates).subList(i2, coordinates.length - 1));
    path2.addAll(Arrays.asList(coordinates).subList(0, i1 + 1));

    return List.of(path1, path2);
  }

  private static List<Coordinate> cleanAndSimplify(List<Coordinate> coordinates) {
    if (coordinates.size() < 2) return coordinates;

    var array = coordinates.toArray(Coordinate[]::new);
    var line = geometryFactory.createLineString(array);
    var simplified = DouglasPeuckerSimplifier.simplify(line, SIMPLIFICATION);
    return new ArrayList<>(Arrays.asList(simplified.getCoordinates()));
  }

  private static double avgDistance(List<Coordinate> coordinates, LineString line) {
    return coordinates.stream()
        .mapToDouble(
            coordinate -> {
              var point = new LasPointGeometry(coordinate);
              return line.distance(point);
            })
        .average()
        .orElseThrow();
  }

  private record TwoIndexes(int a, int b) {}

  private static LasPointGeometry centroid(Coordinate[] coordinates) {
    return GeometryUtilities.centroid(toPoints(coordinates));
  }

  private static List<LasPointGeometry> toPoints(Coordinate[] coordinates) {
    return Arrays.stream(coordinates).map(LasPointGeometry::new).toList();
  }
}
