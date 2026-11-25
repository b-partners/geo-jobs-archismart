package app.bpartners.geojobs.service;

import static java.lang.Math.cos;
import static java.lang.Math.toRadians;

import app.bpartners.geojobs.repository.model.Feature;
import app.bpartners.geojobs.service.dashboard.AnalysisProjectKeys;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.StreamSupport;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.CoordinateFilter;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LinearRing;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.geom.prep.PreparedGeometryFactory;

public final class ZoneGeometry {
  public static final double METERS_PER_DEGREE = 111_320.0;
  public static final int MAX_COORDINATES = 1_000;
  private static final int BUFFER_QUADRANT_SEGMENTS = 4;
  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
  private static final GeometryFactory FACTORY = new GeometryFactory();

  private final Geometry geometry;

  private ZoneGeometry(Geometry geometry) {
    this.geometry = geometry;
  }

  public static Optional<ZoneGeometry> ofIndividual(List<Feature> features) {
    if (!AnalysisProjectKeys.isIndividual(features)) {
      return Optional.empty();
    }
    return features.stream()
        .filter(Objects::nonNull)
        .map(Feature::getGeometry)
        .filter(Objects::nonNull)
        .findFirst()
        .flatMap(ZoneGeometry::parse)
        .filter(parsed -> parsed.getNumPoints() <= MAX_COORDINATES)
        .map(ZoneGeometry::new);
  }

  public ZoneBounds bounds() {
    var envelope = geometry.getEnvelopeInternal();
    return new ZoneBounds(
        envelope.getMinX(), envelope.getMinY(), envelope.getMaxX(), envelope.getMaxY());
  }

  public double iouWith(ZoneGeometry other) {
    var cosLat = cosOfCentroidLatitude();
    var self = projected(geometry, cosLat);
    var that = projected(other.geometry, cosLat);
    var intersection = self.intersection(that).getArea();
    var union = self.getArea() + that.getArea() - intersection;
    return union <= 0 ? 0 : intersection / union;
  }

  public boolean isWithinDisplacementOf(ZoneGeometry other, double maxDisplacementInMeters) {
    var cosLat = cosOfCentroidLatitude();
    var self = projected(geometry, cosLat);
    var that = projected(other.geometry, cosLat);
    return covers(self.buffer(maxDisplacementInMeters, BUFFER_QUADRANT_SEGMENTS), that)
        && covers(that.buffer(maxDisplacementInMeters, BUFFER_QUADRANT_SEGMENTS), self);
  }

  private static boolean covers(Geometry container, Geometry content) {
    return PreparedGeometryFactory.prepare(container).covers(content);
  }

  private double cosOfCentroidLatitude() {
    return cos(toRadians(geometry.getCentroid().getY()));
  }

  private static Geometry projected(Geometry source, double cosLat) {
    var copy = source.copy();
    copy.apply(
        (CoordinateFilter)
            coordinate -> {
              coordinate.x = coordinate.x * cosLat * METERS_PER_DEGREE;
              coordinate.y = coordinate.y * METERS_PER_DEGREE;
            });
    copy.geometryChanged();
    return copy;
  }

  private static Optional<Geometry> parse(Feature.FeatureGeometry featureGeometry) {
    var raw = featureGeometry.getActualInstanceStringValue();
    if (raw == null) {
      return Optional.empty();
    }
    try {
      var root = OBJECT_MAPPER.readTree(raw);
      var coordinates = root.path("coordinates");
      var type = root.path("type").asText("").replace("_", "").toLowerCase();
      Geometry parsed =
          switch (type) {
            case "polygon" -> polygon(coordinates);
            case "multipolygon" ->
                FACTORY.createMultiPolygon(
                    StreamSupport.stream(coordinates.spliterator(), false)
                        .map(ZoneGeometry::polygon)
                        .toArray(Polygon[]::new));
            default -> null;
          };
      if (parsed == null || parsed.isEmpty()) {
        return Optional.empty();
      }
      return Optional.of(parsed.isValid() ? parsed : parsed.buffer(0));
    } catch (Exception e) {
      return Optional.empty();
    }
  }

  private static Polygon polygon(JsonNode rings) {
    var linearRings = new ArrayList<LinearRing>();
    rings.forEach(ring -> linearRings.add(linearRing(ring)));
    var holes = linearRings.subList(1, linearRings.size()).toArray(LinearRing[]::new);
    return FACTORY.createPolygon(linearRings.getFirst(), holes);
  }

  private static LinearRing linearRing(JsonNode ring) {
    var coordinates = new ArrayList<Coordinate>();
    ring.forEach(
        point ->
            coordinates.add(new Coordinate(point.path(0).asDouble(), point.path(1).asDouble())));
    if (!coordinates.getFirst().equals2D(coordinates.getLast())) {
      coordinates.add(coordinates.getFirst().copy());
    }
    return FACTORY.createLinearRing(coordinates.toArray(Coordinate[]::new));
  }
}
