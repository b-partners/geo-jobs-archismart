package app.bpartners.geojobs.model.lidar.planes.postprocessing;

import static app.bpartners.geojobs.model.geometry.GeometryFactory.geometryFactory;
import static app.bpartners.geojobs.model.lidar.planes.algorithm.GeometryUtilities.intersection;
import static app.bpartners.geojobs.model.lidar.planes.postprocessing.ChimneyFixer.getMaxZ;

import app.bpartners.geojobs.model.geometry.PolylineSimplifier;
import app.bpartners.geojobs.model.lidar.LasPointGeometry;
import app.bpartners.geojobs.model.lidar.planes.Plane3D;
import app.bpartners.geojobs.model.lidar.planes.postprocessing.model.ChimneyPlane3D;
import java.util.*;
import java.util.function.BiFunction;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;

@Slf4j
public class DelimitationFiller
    implements BiFunction<Collection<Plane3D>, Collection<LasPointGeometry>, List<Plane3D>> {
  private final int maxEmptyCell;
  private final double gridRadius;
  private final int minCellPointsSize;

  private static final double PLANE_MIN_AREA = 3;
  private static final double MAX_INTERSECTION_AREA = 0.3;

  public DelimitationFiller(int maxEmptyCell, int minCellPointsSize, double gridRadius) {
    this.gridRadius = gridRadius;
    this.maxEmptyCell = maxEmptyCell;
    this.minCellPointsSize = minCellPointsSize;
  }

  @Override
  public List<Plane3D> apply(Collection<Plane3D> planes, Collection<LasPointGeometry> points) {
    var grid = createGrid(points);
    return planes.stream()
        .map(
            plane -> {
              if (plane instanceof ChimneyPlane3D) {
                return plane;
              }
              return compute(plane, grid, planes);
            })
        .toList();
  }

  public Plane3D apply(Plane3D planes, Collection<LasPointGeometry> points) {
    return apply(List.of(planes), points).getFirst();
  }

  private boolean containsAnotherPlane(Plane3D plane, Collection<Plane3D> planes) {
    var delimitation = plane.getConvexDelimitation();
    for (var other : planes) {
      if (other == plane) continue;
      if (other instanceof ChimneyPlane3D) continue;

      var otherDelimitation = other.getDelimitation();
      if (otherDelimitation.getArea() < PLANE_MIN_AREA) continue;

      var intersection = intersection(delimitation, otherDelimitation);
      if (intersection.isEmpty() || intersection instanceof Point) continue;
      if (intersection.getArea() < MAX_INTERSECTION_AREA) continue;

      var point = getMaxZ(otherDelimitation);
      var otherZ = point.getZ();
      var planeZ = plane.zAt(point.getX(), point.getY());
      if (otherZ > planeZ) continue;

      return true;
    }
    return false;
  }

  private Plane3D compute(Plane3D plane, Map<Cell, Cell> grid, Collection<Plane3D> planes) {
    if (!isFullOfPoints(plane, grid)) {
      return plane;
    }

    if (containsAnotherPlane(plane, planes)) {
      return plane;
    }

    var newDelimitation =
        new PolylineSimplifier(plane.getDelimitationConf().simplificationEpsilon())
            .simplifyPolygon(plane.getConvexDelimitation());
    return plane.toBuilder().area(null).delimitation(newDelimitation).build();
  }

  private boolean isFullOfPoints(Plane3D plane, Map<Cell, Cell> grid) {
    var convexPolygon = plane.getConvexDelimitation();
    var envelope = convexPolygon.getEnvelopeInternal();
    var minX = envelope.getMinX();
    var maxX = envelope.getMaxX();
    var minY = envelope.getMinY();
    var maxY = envelope.getMaxY();

    var minIx = getIndex(minX);
    var maxIx = getIndex(maxX);
    var minIy = getIndex(minY);
    var maxIy = getIndex(maxY);

    int totalInside = 0;
    int emptyInside = 0;

    for (int ix = minIx; ix <= maxIx; ix++) {
      for (int iy = minIy; iy <= maxIy; iy++) {
        double centerX = (ix + 0.5) * gridRadius;
        double centerY = (iy + 0.5) * gridRadius;

        if (!isPointsInsidePolygon(centerX, centerY, convexPolygon)) {
          continue;
        }

        totalInside++;

        var cell = grid.computeIfAbsent(new Cell(ix, iy), key -> key);
        if (cell.points().size() < minCellPointsSize) {
          emptyInside++;
        }

        if (emptyInside > maxEmptyCell) {
          return false;
        }
      }
    }

    if (totalInside == 0) {
      return false;
    }

    return emptyInside < maxEmptyCell;
  }

  private static boolean isPointsInsidePolygon(double x, double y, Polygon polygon) {
    var point = geometryFactory.createPoint(new Coordinate(x, y));
    return polygon.contains(point);
  }

  private Map<Cell, Cell> createGrid(Collection<LasPointGeometry> points) {
    Map<Cell, Cell> grid = new HashMap<>();
    for (var point : points) {
      int ix = getIndex(point.getX());
      int iy = getIndex(point.getY());
      grid.computeIfAbsent(new Cell(ix, iy), k -> new Cell(ix, iy)).points().add(point);
    }
    return grid;
  }

  private int getIndex(double value) {
    return (int) Math.floor(value / gridRadius);
  }

  private record Cell(int x, int y, @EqualsAndHashCode.Exclude List<LasPointGeometry> points) {
    public Cell(int x, int y) {
      this(x, y, new ArrayList<>());
    }
  }
}
