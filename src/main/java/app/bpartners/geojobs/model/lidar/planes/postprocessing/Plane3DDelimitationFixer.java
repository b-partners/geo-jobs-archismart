package app.bpartners.geojobs.model.lidar.planes.postprocessing;

import static app.bpartners.geojobs.model.geometry.GeometryFactory.geometryFactory;

import app.bpartners.geojobs.model.geometry.PolylineSimplifier;
import app.bpartners.geojobs.model.lidar.LasPointGeometry;
import app.bpartners.geojobs.model.lidar.planes.Plane3D;
import java.util.*;
import java.util.function.BiFunction;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Polygon;

@Slf4j
public class Plane3DDelimitationFixer
    implements BiFunction<Plane3D, Collection<LasPointGeometry>, Plane3D> {
  private final double gridRadius;
  private final double simplificationEpsilon;
  private final int maxEmptyCell;
  private final int minCellPointsSize;

  public Plane3DDelimitationFixer(
      int maxEmptyCell, int minCellPointsSize, double gridRadius, double simplificationEpsilon) {
    this.gridRadius = gridRadius;
    this.maxEmptyCell = maxEmptyCell;
    this.minCellPointsSize = minCellPointsSize;
    this.simplificationEpsilon = simplificationEpsilon;
  }

  public List<Plane3D> apply(List<Plane3D> planes, Collection<LasPointGeometry> points) {
    var grid = createGrid(points);
    return planes.stream().map(plane -> apply(plane, grid)).toList();
  }

  private Plane3D apply(Plane3D plane, Map<Cell, Cell> grid) {
    if (!isFullOfPoints(plane, grid)) {
      return plane;
    }

    var delimitation =
        new PolylineSimplifier(simplificationEpsilon)
            .simplifyPolygon(plane.getConvexDelimitation());
    return plane.toBuilder().delimitation(delimitation).build();
  }

  @Override
  public Plane3D apply(Plane3D plane, Collection<LasPointGeometry> points) {
    var grid = createGrid(points);
    return apply(plane, grid);
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
