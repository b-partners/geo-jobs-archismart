package app.bpartners.geojobs.model.lidar.planes.model;

import static app.bpartners.geojobs.model.lidar.planes.algorithm.GeometryUtilities.getLargestPolygon;

import java.util.function.BiFunction;
import lombok.RequiredArgsConstructor;
import org.locationtech.jts.geom.Polygon;

@RequiredArgsConstructor
public class RoofPointsDelimitationTransformer
    implements BiFunction<LasRoofDelimitationType, Polygon, Polygon> {
  private final double roofFacesBuffer;

  @Override
  public Polygon apply(LasRoofDelimitationType type, Polygon polygon) {
    return switch (type) {
      case ROOF_SEGMENT_FACE_DELIMITATION -> getLargestPolygon(polygon.buffer(roofFacesBuffer));
      default -> polygon;
    };
  }

  public static RoofPointsDelimitationTransformer none() {
    return new RoofPointsDelimitationTransformer(0);
  }
}
