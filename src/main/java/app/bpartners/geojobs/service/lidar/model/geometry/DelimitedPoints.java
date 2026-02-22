package app.bpartners.geojobs.service.lidar.model.geometry;

import app.bpartners.geojobs.model.lidar.LasPointGeometry;
import java.util.HashSet;
import java.util.Set;
import lombok.Builder;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;

@Builder(toBuilder = true)
public record DelimitedPoints(
    Geometry boundaryEPSG4326,
    Geometry boundaryLambert93,
    Envelope boundaryEPSG4326Envelope,
    Envelope boundaryLambert93Envelope,
    Set<LasPointGeometry> points) {
  public DelimitedPoints(
      Geometry boundaryEPSG4326, Geometry boundaryLambert93, Set<LasPointGeometry> points) {
    this(
        boundaryEPSG4326,
        boundaryLambert93,
        boundaryEPSG4326 != null ? boundaryEPSG4326.getEnvelopeInternal() : null,
        boundaryLambert93 != null ? boundaryLambert93.getEnvelopeInternal() : null,
        points);
  }

  public static DelimitedPoints empty(Geometry boundaryEPSG4326, Geometry boundaryLambert93) {
    return new DelimitedPoints(boundaryEPSG4326, boundaryLambert93, new HashSet<>());
  }
}
