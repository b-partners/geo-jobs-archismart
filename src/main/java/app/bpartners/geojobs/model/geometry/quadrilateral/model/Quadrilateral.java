package app.bpartners.geojobs.model.geometry.quadrilateral.model;

import static app.bpartners.geojobs.model.geometry.GeometryFactory.geometryFactory;
import static java.util.stream.Collectors.toSet;

import app.bpartners.geojobs.model.geometry.IntXY;
import app.bpartners.geojobs.model.geometry.polygon.PolygonOrientation;
import java.util.Arrays;
import java.util.Collection;
import java.util.Set;
import lombok.Getter;
import lombok.experimental.Accessors;
import org.locationtech.jts.algorithm.hull.ConcaveHull;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;

@Accessors(fluent = true)
@Getter
public class Quadrilateral {
  private final Polygon polygon;

  public Quadrilateral(Polygon p) {
    this(Arrays.stream(p.getCoordinates()).collect(toSet()));
  }

  public Quadrilateral(Set<Coordinate> coordinates) {
    checkQuadrilateral(coordinates);
    this.polygon =
        // _not_ convex hull as edges will be swallowed by
        // the (over-approximation of the) convex hull if quadrilateral is concave
        (Polygon)
            new ConcaveHull(
                    geometryFactory.createMultiPointFromCoords(
                        coordinates.toArray(new Coordinate[0])))
                .getHull();
  }

  public static Quadrilateral fromIntXYCoordinates(Set<IntXY> coordinates) {
    return new Quadrilateral(
        coordinates.stream().map(xy -> new Coordinate(xy.x(), xy.y())).collect(toSet()));
  }

  private void checkQuadrilateral(Collection<Coordinate> coordinates) {
    if (coordinates.size() != 4) {
      throw new IllegalArgumentException(
          "Quadrilateral expects 4 coordinates but got: " + coordinates);
    }
  }

  public Point centroid() {
    return polygon.getCentroid();
  }

  public double angle() {
    return new PolygonOrientation(polygon).get();
  }
}
