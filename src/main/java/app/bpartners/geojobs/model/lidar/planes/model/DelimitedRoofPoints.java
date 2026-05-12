package app.bpartners.geojobs.model.lidar.planes.model;

import static app.bpartners.geojobs.model.geometry.GeometryFactory.geometryFactory;
import static app.bpartners.geojobs.model.lidar.planes.model.DelimitedRoofPointsItem.isOutsideEnvelope;
import static app.bpartners.geojobs.model.lidar.planes.model.LasRoofDelimitationType.ENTIRE_ROOF_DELIMITATION;

import app.bpartners.geojobs.model.lidar.LasPointGeometry;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.*;
import org.locationtech.jts.geom.*;

@Getter
@Builder(toBuilder = true)
@EqualsAndHashCode(callSuper = false)
public class DelimitedRoofPoints extends MultiPolygon {
  @EqualsAndHashCode.Include private final Envelope globalEnvelope;
  @EqualsAndHashCode.Include private final Envelope groundEnvelope;
  @EqualsAndHashCode.Include private final LasRoofDelimitationType type;
  @EqualsAndHashCode.Exclude private final Geometry originalInEPSG4336;
  @EqualsAndHashCode.Exclude private final DelimitedRoofPointsItem[] items;
  @EqualsAndHashCode.Exclude private final Set<LasPointGeometry> groundPoints;

  private static final double GROUND_BUFFER = 3;
  private static final double MAX_GROUND_POINTS_COUNT = 100;

  public DelimitedRoofPoints(
      Envelope globalEnvelope,
      Envelope groundEnvelope,
      LasRoofDelimitationType type,
      Geometry originalInEPSG4336,
      DelimitedRoofPointsItem[] items,
      Set<LasPointGeometry> groundPoints) {
    super(toPolygons(items), geometryFactory);

    this.type = type;
    this.items = items;
    this.originalInEPSG4336 = originalInEPSG4336;
    this.groundPoints = groundPoints;
    this.globalEnvelope = globalEnvelope;
    this.groundEnvelope = groundEnvelope;
  }

  public DelimitedRoofPoints(
      LasRoofDelimitationType type,
      Geometry originalInEPSG4336,
      Geometry delimitation,
      RoofPointsDelimitationTransformer transformer) {
    this(type, originalInEPSG4336, getPolygons(delimitation), transformer);
  }

  public DelimitedRoofPoints(
      LasRoofDelimitationType type,
      Geometry originalInEPSG4336,
      Polygon[] polygons,
      RoofPointsDelimitationTransformer transformer) {
    super(polygons, geometryFactory);

    this.type = type;
    this.groundPoints = new HashSet<>();
    this.originalInEPSG4336 = originalInEPSG4336;
    this.items = toItems(type, polygons, transformer);
    this.globalEnvelope = super.getEnvelopeInternal();
    this.groundEnvelope = toBufferedEnvelope(globalEnvelope);
  }

  public boolean addRoofPointIfInside(LasPointGeometry point) {
    if (isOutsideEnvelope(this.globalEnvelope, point)) {
      return false;
    }

    boolean atLeastInsideOneItem = false;
    for (var item : this.items) {
      if (item.add(point)) atLeastInsideOneItem = true;
    }
    return atLeastInsideOneItem;
  }

  public boolean addGroundPointIfInside(LasPointGeometry point) {
    if (this.groundPoints.size() > MAX_GROUND_POINTS_COUNT) {
      return false;
    }

    if (isOutsideEnvelope(this.groundEnvelope, point)) {
      return false;
    }

    this.groundPoints.add(point);
    return true;
  }

  public DelimitedRoofPoints merge(DelimitedRoofPoints other) {
    var mergedItems = getMergedItems(this, other);

    var mergedGroundPoints = new HashSet<>(this.groundPoints);
    mergedGroundPoints.addAll(other.getGroundPoints());

    return this.toBuilder().groundPoints(mergedGroundPoints).items(mergedItems).build();
  }

  private static DelimitedRoofPointsItem[] getMergedItems(
      DelimitedRoofPoints left, DelimitedRoofPoints right) {
    var n = left.getItems().length;

    var merged = new DelimitedRoofPointsItem[n];
    for (int i = 0; i < n; i++) {
      var leftItem = left.getItems()[i];
      var rightItem = right.getItems()[i];

      var points = new HashSet<>(leftItem.getPoints());
      points.addAll(rightItem.getPoints());

      merged[i] = leftItem.toBuilder().points(points).build();
    }

    return merged;
  }

  private static Envelope toBufferedEnvelope(Envelope envelope) {
    var envelopeAsGeometry = geometryFactory.toGeometry(envelope);
    var buffered = envelopeAsGeometry.buffer(GROUND_BUFFER);
    return buffered.getEnvelopeInternal();
  }

  private static DelimitedRoofPointsItem[] toItems(
      LasRoofDelimitationType type,
      Polygon[] polygons,
      RoofPointsDelimitationTransformer transformer) {
    return Arrays.stream(polygons)
        .map(polygon -> new DelimitedRoofPointsItem(type, polygon, transformer))
        .toArray(DelimitedRoofPointsItem[]::new);
  }

  private static Polygon[] getPolygons(Geometry geometry) {
    return switch (geometry) {
      case Polygon polygon -> new Polygon[] {polygon};
      case MultiPolygon multiPolygon -> getPolygonsFromMultiPolygon(multiPolygon);
      default -> throw new IllegalArgumentException("Invalid geometry type");
    };
  }

  private static Polygon[] getPolygonsFromMultiPolygon(MultiPolygon geometries) {
    int n = geometries.getNumGeometries();
    if (n == 0) {
      throw new IllegalArgumentException("Invalid geometry, MultiPolygon is empty");
    }

    var polygons = new Polygon[n];
    for (int i = 0; i < geometries.getNumGeometries(); i++) {
      var geometry = geometries.getGeometryN(i);
      if (!(geometry instanceof Polygon polygon)) {
        throw new IllegalArgumentException("Invalid geometry type");
      }

      polygons[i] = polygon;
    }

    return polygons;
  }

  public static DelimitedRoofPoints empty(Geometry delimitation) {
    return new DelimitedRoofPoints(
        ENTIRE_ROOF_DELIMITATION,
        delimitation,
        delimitation,
        new RoofPointsDelimitationTransformer(0));
  }

  public List<LasPointGeometry> getPoints() {
    return Arrays.stream(this.items)
        .map(DelimitedRoofPointsItem::getPoints)
        .flatMap(Set::stream)
        .toList();
  }

  private static Polygon[] toPolygons(DelimitedRoofPointsItem[] items) {
    return Arrays.stream(items).map(DelimitedRoofPointsItem::getPolygon).toArray(Polygon[]::new);
  }
}
