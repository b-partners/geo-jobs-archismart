package app.bpartners.geojobs.model.lidar;

import static app.bpartners.geojobs.model.geometry.GeometryFactory.geometryFactory;
import static app.bpartners.geojobs.model.lidar.planes.exporter.Plane3DExtractionStep.DELIMITATION_SIMPLIFICATION;
import static app.bpartners.geojobs.model.lidar.planes.exporter.Plane3DExtractionStep.RAW_DELIMITATION_EXTRACTION;

import app.bpartners.geojobs.model.geometry.PolylineSimplifier;
import app.bpartners.geojobs.model.lidar.planes.exporter.Plane3DExtractionStepExporter;
import java.util.*;
import lombok.extern.slf4j.Slf4j;
import org.locationtech.jts.algorithm.hull.ConcaveHull;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Polygon;

@Slf4j
public class LasPointsDelimiter {
  private Polygon polygon;
  private final double concaveRatio;
  private final Collection<LasPointGeometry> points;
  private final PolylineSimplifier polylineSimplifier;
  private final Plane3DExtractionStepExporter exporter;

  public LasPointsDelimiter(
      Collection<LasPointGeometry> points,
      double concaveRatio,
      double polylineSimplifierEpsilon,
      Plane3DExtractionStepExporter exporter) {
    this.points = points;
    this.exporter = exporter;
    this.concaveRatio = concaveRatio;
    this.polylineSimplifier = new PolylineSimplifier(polylineSimplifierEpsilon);
  }

  public LasPointsDelimiter(
      Collection<LasPointGeometry> points, double concaveRatio, double polylineSimplifierEpsilon) {
    this(points, concaveRatio, polylineSimplifierEpsilon, null);
  }

  public Polygon getPolygon() {
    if (polygon == null) {
      var rawDelimitation = getPolygon(points);
      polygon = polylineSimplifier.simplifyPolygon(rawDelimitation);

      if (exporter != null) {
        exporter.export(RAW_DELIMITATION_EXTRACTION, rawDelimitation);
        exporter.export(DELIMITATION_SIMPLIFICATION, polygon);
      }
    }

    return polygon;
  }

  private Polygon getPolygon(Collection<LasPointGeometry> points) {
    var coordinates =
        points.stream().map(LasPointGeometry::getCoordinate).toArray(Coordinate[]::new);
    var multiPoint = geometryFactory.createMultiPointFromCoords(coordinates);

    var hull = ConcaveHull.concaveHullByLengthRatio(multiPoint, concaveRatio);

    if (!hull.isValid()) {
      log.warn("Concave hull produced an invalid geometry. Attempting to fix it with buffer(0).");
      hull = hull.buffer(0);
    }

    return (Polygon) hull;
  }
}
