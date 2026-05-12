package app.bpartners.geojobs.model.lidar.planes;

import static app.bpartners.geojobs.model.lidar.planes.algorithm.PointsDelimitationComputer.getConcave;
import static app.bpartners.geojobs.model.lidar.planes.exporter.Plane3DExtractionStep.*;

import app.bpartners.geojobs.model.geometry.PolylineSimplifier;
import app.bpartners.geojobs.model.geometry.lr.LrSimplifier;
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
  private final Collection<LasPointGeometry> points;
  private final Plane3DExtractionStepExporter exporter;

  public PlaneDelimitation(
      PlaneDelimitationConf conf,
      Collection<LasPointGeometry> points,
      Plane3DExtractionStepExporter exporter) {
    this.conf = conf;
    this.points = points;
    this.exporter = exporter;
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
      polygon = simplify(rawDelimitation, conf, exporter);
    }

    return polygon;
  }

  public static Polygon simplify(
      Polygon delimitation, PlaneDelimitationConf conf, Plane3DExtractionStepExporter exporter) {
    var dpsSimplifier = new PolylineSimplifier(conf.dpsEpsilon());
    var lrSimplifier = new LrSimplifier(conf.lrDegEpsilon());
    var dpsPolygon = dpsSimplifier.simplifyPolygon(delimitation);

    if (exporter != null) {
      var lrPolygon = lrSimplifier.apply(dpsPolygon);
      exporter.export(RAW_DELIMITATION_EXTRACTION, delimitation);
      exporter.export(DPS_SIMPLIFICATION, dpsPolygon);
      exporter.export(LR_SIMPLIFICATION, lrPolygon);
    }

    return dpsPolygon;
  }

  @Builder(toBuilder = true)
  public record PlaneDelimitationConf(
      RangedConf<Integer, Double> concaveRatio, double dpsEpsilon, double lrDegEpsilon) {}
}
