package app.bpartners.geojobs.model.lidar.planes.model;

import app.bpartners.geojobs.model.lidar.LasPointGeometry;
import java.util.HashSet;
import java.util.Set;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Polygon;

@Builder(toBuilder = true)
@RequiredArgsConstructor
public class DelimitedRoofPointsItem {
  @Getter private final Polygon polygon;
  @Getter private final Set<LasPointGeometry> points;
  @Getter private final LasRoofDelimitationType type;

  private final Polygon transformedPolygon;
  private final Envelope transformedEnvelope;

  public DelimitedRoofPointsItem(
      LasRoofDelimitationType type,
      Polygon polygon,
      RoofPointsDelimitationTransformer transformer) {
    this.type = type;
    this.polygon = polygon;
    this.points = new HashSet<>();
    this.transformedPolygon = transformer.apply(type, polygon);
    this.transformedEnvelope = this.transformedPolygon.getEnvelopeInternal();
  }

  boolean add(LasPointGeometry point) {
    if (isOutsideEnvelope(this.transformedEnvelope, point)) {
      return false;
    }

    if (!this.transformedPolygon.contains(point)) {
      return false;
    }

    this.points.add(point);
    return true;
  }

  static boolean isOutsideEnvelope(Envelope envelope, LasPointGeometry point) {
    double x = point.getX();
    double y = point.getY();

    return x < envelope.getMinX()
        || x > envelope.getMaxX()
        || y < envelope.getMinY()
        || y > envelope.getMaxY();
  }
}
