package app.bpartners.geojobs.service.lidar;

import app.bpartners.geojobs.model.lidar.planes.model.DelimitedRoofPoints;
import java.util.Map;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;

public record PointsExtractionResult(Map<Envelope, DelimitedRoofPoints> points) {
  public DelimitedRoofPoints extract(Geometry delimitation) {
    return points.getOrDefault(
        delimitation.getEnvelopeInternal(), DelimitedRoofPoints.empty(delimitation));
  }
}
