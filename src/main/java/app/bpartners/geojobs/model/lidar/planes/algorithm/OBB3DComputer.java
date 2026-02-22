package app.bpartners.geojobs.model.lidar.planes.algorithm;

import static app.bpartners.geojobs.model.geometry.GeometryFactory.geometryFactory;

import app.bpartners.geojobs.model.lidar.planes.Plane3D;
import java.util.Arrays;
import java.util.function.Function;
import lombok.RequiredArgsConstructor;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Polygon;

@RequiredArgsConstructor
public class OBB3DComputer implements Function<Plane3D, Polygon> {
  private final OBB2DComputer obb2DComputer;

  public OBB3DComputer() {
    this(new OBB2DComputer());
  }

  @Override
  public Polygon apply(Plane3D plane) {
    var obb2D = obb2DComputer.apply(plane);
    var polygon2D = obb2D.toPolygon();

    var center = obb2D.center();
    var isPlaneVertical = plane.isVertical();
    var coordinates3D =
        Arrays.stream(polygon2D.getCoordinates())
            .map(
                coordinate -> {
                  var x = coordinate.getX();
                  var y = coordinate.getY();
                  var z = center.getZ();

                  if (!isPlaneVertical) {
                    z = plane.zAt(x, y);
                  }

                  return new Coordinate(x, y, z);
                })
            .toArray(Coordinate[]::new);
    return geometryFactory.createPolygon(coordinates3D);
  }
}
