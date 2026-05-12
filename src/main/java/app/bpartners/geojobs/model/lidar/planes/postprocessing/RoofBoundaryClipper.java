package app.bpartners.geojobs.model.lidar.planes.postprocessing;

import static app.bpartners.geojobs.model.lidar.planes.algorithm.GeometryUtilities.getLargestPolygon;
import static app.bpartners.geojobs.model.lidar.planes.algorithm.GeometryUtilities.project;

import app.bpartners.geojobs.model.lidar.planes.Plane3D;
import app.bpartners.geojobs.model.lidar.planes.postprocessing.model.ChimneyPlane3D;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;
import org.locationtech.jts.geom.Polygon;

public class RoofBoundaryClipper implements Function<Collection<Plane3D>, List<Plane3D>> {
  private final Polygon roofDelimitation;

  public RoofBoundaryClipper(Polygon roofDelimitation) {
    if (!roofDelimitation.isValid()) {
      this.roofDelimitation = (Polygon) roofDelimitation.buffer(0);
    } else {
      this.roofDelimitation = roofDelimitation;
    }
  }

  private static final double MIN_POLYGON_AREA_TO_CLIP = 8;

  @Override
  public List<Plane3D> apply(Collection<Plane3D> planes) {
    return planes.stream()
        .map(
            plane -> {
              if (plane instanceof ChimneyPlane3D) {
                return plane;
              }
              if (plane.get2DArea() < MIN_POLYGON_AREA_TO_CLIP) {
                return plane;
              }

              var newPolygon = clip(plane.getDelimitation());
              newPolygon = project(plane, newPolygon);

              return plane.toBuilder()
                  .area(null)
                  .convexDelimitation(null)
                  .delimitation(newPolygon)
                  .build();
            })
        .toList();
  }

  private Polygon clip(Polygon polygon) {
    if (!polygon.isValid()) polygon = getLargestPolygon(polygon.buffer(0));
    var intersection = polygon.intersection(roofDelimitation);

    if (intersection.isEmpty()) {
      return polygon;
    }
    return getLargestPolygon(intersection);
  }
}
