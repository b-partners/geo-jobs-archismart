package app.bpartners.geojobs.model.lidar.planes;

import static app.bpartners.geojobs.model.lidar.planes.algorithm.PointsDelimitationComputer.getConcave;
import static app.bpartners.geojobs.model.lidar.planes.exporter.Plane3DExtractionStep.DELIMITATION_SIMPLIFICATION;
import static app.bpartners.geojobs.model.lidar.planes.exporter.Plane3DExtractionStep.RAW_DELIMITATION_EXTRACTION;

import app.bpartners.geojobs.model.geometry.PolylineSimplifier;
import app.bpartners.geojobs.model.lidar.LasPointGeometry;
import app.bpartners.geojobs.model.lidar.planes.conf.RangedConf;
import app.bpartners.geojobs.model.lidar.planes.exporter.Plane3DExtractionStepExporter;
import java.util.*;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;
import org.locationtech.jts.geom.Polygon;

@Slf4j
public class PlaneDelimitation {
  private Polygon polygon;
  private final PlaneDelimitationConf conf;
  private final PolylineSimplifier polylineSimplifier;
  private final Collection<LasPointGeometry> points;
  private final Plane3DExtractionStepExporter exporter;

  public PlaneDelimitation(
      PlaneDelimitationConf conf,
      Collection<LasPointGeometry> points,
      Plane3DExtractionStepExporter exporter) {
    this.conf = conf;
    this.points = points;
    this.exporter = exporter;
    this.polylineSimplifier = new PolylineSimplifier(conf.simplificationEpsilon());
  }

  public PlaneDelimitation(PlaneDelimitationConf conf, Collection<LasPointGeometry> points) {
    this(conf, points, null);
  }

  private double getConcaveRatioValue() {
    return conf.concaveRatio().getValue(this.points.size());
  }

  public Polygon getPolygon() {
    if (polygon == null) {
      var rawDelimitation = getConcave(points, getConcaveRatioValue());
      polygon = polylineSimplifier.simplifyPolygon(rawDelimitation);

      if (exporter != null) {
        exporter.export(RAW_DELIMITATION_EXTRACTION, rawDelimitation);
        exporter.export(DELIMITATION_SIMPLIFICATION, polygon);
      }
    }

    return polygon;
  }

  @Builder(toBuilder = true)
  public record PlaneDelimitationConf(
      RangedConf<Integer, Double> concaveRatio, double simplificationEpsilon) {}
}
