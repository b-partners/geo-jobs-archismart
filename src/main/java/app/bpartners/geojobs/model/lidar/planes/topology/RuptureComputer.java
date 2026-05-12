package app.bpartners.geojobs.model.lidar.planes.topology;

import static app.bpartners.geojobs.model.geometry.GeometryFactory.geometryFactory;
import static app.bpartners.geojobs.model.lidar.planes.algorithm.GeometryUtilities.*;
import static app.bpartners.geojobs.model.lidar.planes.topology.algorithm.Plane3DIntersection.intersects;
import static app.bpartners.geojobs.model.lidar.planes.topology.algorithm.PolygonSmartUnion.union;

import app.bpartners.geojobs.model.lidar.planes.Plane3D;
import app.bpartners.geojobs.model.lidar.planes.topology.model.Line3D;
import app.bpartners.geojobs.model.lidar.planes.topology.model.Rupture;
import java.util.*;
import java.util.function.BiFunction;
import lombok.extern.slf4j.Slf4j;
import org.locationtech.jts.densify.Densifier;
import org.locationtech.jts.geom.*;
import org.locationtech.jts.linearref.LengthIndexedLine;

@Slf4j
public class RuptureComputer implements BiFunction<Plane3D, Plane3D, Optional<Rupture>> {
  private static final double EXTENSION = 20;
  private static final double MAX_DISTANCE = 1;
  private static final double DENSIFIED_DISTANCE = 0.3;
  private static final double MIN_INTERSECTION_DISTANCE = 3;

  @Override
  public Optional<Rupture> apply(Plane3D a, Plane3D b) {
    var optionalLine3D = intersects(a, b);
    if (optionalLine3D.isEmpty()) {
      return Optional.empty();
    }

    var line3D = optionalLine3D.get();
    var optionalRuptureLine = getRuptureLine(line3D, a, b);
    if (optionalRuptureLine.isEmpty()) {
      return Optional.empty();
    }

    var ruptureLine = optionalRuptureLine.get();
    if (ruptureLine.distance(a.getDelimitation()) > MAX_DISTANCE) {
      return Optional.empty();
    } else if (ruptureLine.distance(b.getDelimitation()) > MAX_DISTANCE) {
      return Optional.empty();
    }

    var baseRupturePoints = List.of(ruptureLine.getCoordinates());
    return Optional.of(
        Rupture.builder()
            .line(ruptureLine)
            .points(baseRupturePoints)
            .endIntersection(new HashSet<>())
            .startIntersection(new HashSet<>())
            .build());
  }

  // TODO: find a better way
  private static final double LONG_LINE_LENGTH = 100_000_000;

  private static LineString getLongLine(Line3D line) {
    var start =
        new Coordinate(
            line.point().getX() - line.direction().getX() * LONG_LINE_LENGTH,
            line.point().getY() - line.direction().getY() * LONG_LINE_LENGTH);
    var end =
        new Coordinate(
            line.point().getX() + line.direction().getX() * LONG_LINE_LENGTH,
            line.point().getY() + line.direction().getY() * LONG_LINE_LENGTH);
    return geometryFactory.createLineString(new Coordinate[] {start, end});
  }

  private static Optional<LineString> getRuptureLine(Line3D line, Plane3D a, Plane3D b) {
    var reallyLongLine = getLongLine(line);
    var optionalUnion =
        union(a.getDelimitation(), b.getDelimitation(), MAX_DISTANCE, MIN_INTERSECTION_DISTANCE);

    if (optionalUnion.isEmpty()) {
      return Optional.empty();
    }

    var union = optionalUnion.get();
    var intersection = union.intersection(reallyLongLine);
    if (intersection.isEmpty() || intersection.getCoordinates().length < 2) {
      return Optional.empty();
    }

    if (intersection.getLength() < MIN_INTERSECTION_DISTANCE) {
      return Optional.empty();
    }

    var splitter = geometryFactory.createLineString(intersection.getCoordinates());
    var extendedSplitter = extend(splitter, EXTENSION);
    var maxLineProjectedByA = maxLineProjected(extendedSplitter, a);
    var maxLineProjectedByB = maxLineProjected(extendedSplitter, b);
    var expectedLine =
        maxLineProjectedByA.getLength() < maxLineProjectedByB.getLength()
            ? maxLineProjectedByA
            : maxLineProjectedByB;

    if (splitter.getLength() > expectedLine.getLength()) {
      splitter = expectedLine;
    }

    var coordinatesWithZ = project(a, splitter.getCoordinates());
    splitter = geometryFactory.createLineString(coordinatesWithZ);
    return Optional.of(splitter);
  }

  private static LineString maxLineProjected(LineString line, Plane3D plane) {
    var indexed = new LengthIndexedLine(line);
    var densified = Densifier.densify(plane.getDelimitation(), DENSIFIED_DISTANCE);
    var coordinates = project(plane, densified.getCoordinates());

    double minIndex = Double.POSITIVE_INFINITY;
    double maxIndex = Double.NEGATIVE_INFINITY;
    for (var coordinate : coordinates) {
      double index = indexed.project(coordinate);
      if (index < minIndex) minIndex = index;
      if (index > maxIndex) maxIndex = index;
    }

    var a = indexed.extractPoint(minIndex);
    var b = indexed.extractPoint(maxIndex);
    return geometryFactory.createLineString(new Coordinate[] {a, b});
  }
}
