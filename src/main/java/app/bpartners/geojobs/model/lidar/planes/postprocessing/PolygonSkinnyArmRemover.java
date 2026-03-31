package app.bpartners.geojobs.model.lidar.planes.postprocessing;

import static app.bpartners.geojobs.model.geometry.GeometryFactory.geometryFactory;
import static app.bpartners.geojobs.model.lidar.planes.exporter.Plane3DExtractionStep.*;
import static java.util.Comparator.comparingDouble;

import app.bpartners.geojobs.model.lidar.planes.algorithm.OBB2DComputer;
import app.bpartners.geojobs.model.lidar.planes.exporter.Plane3DExtractionStepExporter;
import java.util.*;
import java.util.function.Function;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import org.locationtech.jts.algorithm.construct.MaximumInscribedCircle;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.geom.util.GeometryCombiner;
import org.locationtech.jts.simplify.TopologyPreservingSimplifier;

@RequiredArgsConstructor
public class PolygonSkinnyArmRemover implements Function<Polygon, Polygon> {
  private final PolygonSkinnyArmRemoverConf conf;
  private final Plane3DExtractionStepExporter exporter;

  @Override
  public Polygon apply(Polygon polygon) {
    if (polygon.getArea() <= conf.minAreaToCheck()) {
      return polygon;
    }

    var grid = createGrid(polygon);
    var gridClassification = classify(grid);

    if (exporter != null) {
      export(gridClassification, grid);
    }

    if (gridClassification.toDelete().isEmpty()) {
      return polygon;
    }

    var toDelete = getGeometry(gridClassification.toDelete(), grid);
    var fixedPolygon = polygon.difference(toDelete).buffer(0.1).buffer(-0.1);
    var finalPolygon = getLargestPolygon(fixedPolygon);

    if (exporter != null) {
      exporter.export(BEFORE_REMOVING_SKINNY_ARM, polygon);
      exporter.export(AFTER_REMOVING_SKINNY_ARM, finalPolygon);
    }

    finalPolygon = getLargestPolygon(finalPolygon.buffer(0.1).buffer(-0.1));
    return (Polygon) TopologyPreservingSimplifier.simplify(finalPolygon, 0.05);
  }

  private Grid createGrid(Polygon polygon) {
    var grid = new Grid(new HashMap<>());

    var env = polygon.getEnvelopeInternal();
    int minX = (int) Math.floor(env.getMinX() / conf.gridSize());
    int maxX = (int) Math.ceil(env.getMaxX() / conf.gridSize());
    int minY = (int) Math.floor(env.getMinY() / conf.gridSize());
    int maxY = (int) Math.ceil(env.getMaxY() / conf.gridSize());

    for (int gx = minX; gx <= maxX; gx++) {
      for (int gy = minY; gy <= maxY; gy++) {
        var cell = createCellPolygon(gx, gy, conf.gridSize());

        if (!polygon.intersects(cell)) continue;

        var intersection = polygon.intersection(cell);
        if (!intersection.isEmpty()) {
          grid.put(new CellIndex(gx, gy), intersection);
        }
      }
    }

    return grid;
  }

  private List<Geometry> getValidNeighbors(CellIndex cell, Grid grid) {
    List<Geometry> neighbors = new ArrayList<>();

    for (int dx = -1; dx <= 1; dx++) {
      for (int dy = -1; dy <= 1; dy++) {
        if (dx == 0 && dy == 0) continue;

        var idx = new CellIndex(cell.x() + dx, cell.y() + dy);
        var neighbor = grid.get(idx);

        if (neighbor != null && neighbor.getArea() >= conf.cellMin2DArea()) {
          neighbors.add(neighbor);
        }
      }
    }

    return neighbors;
  }

  private GridClassification classify(Grid grid) {
    var baseClassification = getBaseClassification(grid);
    var smallWidthIsExcludeFromToDelete = excludeIfNotWidthIsTooBig(baseClassification, grid);
    var extendedToDelete = extendToDeleteCells(smallWidthIsExcludeFromToDelete);
    var withoutStandaloneToKeepCells = addStandaloneToKeepCellToDelete(extendedToDelete, grid);
    return excludeIfNotHeightIsTooSmall(withoutStandaloneToKeepCells, grid);
  }

  private GridClassification getBaseClassification(Grid grid) {
    Set<CellIndex> toKeep = new HashSet<>();
    Set<CellIndex> toDelete = new HashSet<>();
    Set<CellIndex> invalidGrid = new HashSet<>();

    for (var entry : grid.data().entrySet()) {
      var idx = entry.getKey();
      var cellPolygon = entry.getValue();

      if (cellPolygon.getArea() >= conf.cellMin2DArea()) {
        toKeep.add(idx);
        continue;
      }

      invalidGrid.add(idx);
      var neighbors = getValidNeighbors(idx, grid);
      if (neighbors.size() >= conf.cellMinNeighborsCount()) {
        toKeep.add(idx);
      } else {
        toDelete.add(idx);
      }
    }

    return new GridClassification(toKeep, invalidGrid, toDelete);
  }

  private GridClassification addStandaloneToKeepCellToDelete(
      GridClassification classified, Grid grid) {
    var result = classified.copy();

    List<Set<CellIndex>> connectedGroups = getConnected(classified.toKeep());
    var finalToKeep =
        connectedGroups.stream()
            .max(comparingDouble(group -> getGeometry(group, grid).getArea()))
            .orElse(new HashSet<>());

    for (Set<CellIndex> group : connectedGroups) {
      if (group != finalToKeep) {
        result.toDelete().addAll(group);
        result.invalidGrid().addAll(group);
      }
    }

    return result.toBuilder().toKeep(finalToKeep).build();
  }

  private GridClassification excludeIfNotWidthIsTooBig(GridClassification classified, Grid grid) {
    var toDelete = new HashSet<CellIndex>();
    var toKeep = new HashSet<>(classified.toKeep());
    var groups = getConnected(classified.toDelete());

    for (var group : groups) {
      var geometry = getGeometry(group, grid);

      if (isSkinny(geometry)) {
        toDelete.addAll(group);
      } else {
        toKeep.addAll(group);
      }
    }

    return classified.toBuilder().toKeep(toKeep).toDelete(toDelete).build();
  }

  private GridClassification excludeIfNotHeightIsTooSmall(
      GridClassification classified, Grid grid) {
    var toDelete = new HashSet<CellIndex>();
    var toKeep = new HashSet<>(classified.toKeep());
    var groups = getConnected(classified.toDelete());

    for (var group : groups) {
      var geometry = getGeometry(group, grid);

      if (isSkinnyArm(geometry)) {
        toDelete.addAll(group);
      } else {
        toKeep.addAll(group);
      }
    }

    return classified.toBuilder().toKeep(toKeep).toDelete(toDelete).build();
  }

  private static GridClassification extendToDeleteCells(GridClassification base) {
    var extended = base.copy();
    for (var toDelete : new HashSet<>(extended.toDelete())) {
      for (int dx = -1; dx <= 1; dx++) {
        for (int dy = -1; dy <= 1; dy++) {
          if (dx == 0 && dy == 0) continue;
          var neighbor = new CellIndex(toDelete.x() + dx, toDelete.y() + dy);
          if (extended.invalidGrid().contains(neighbor)) {
            extended.toKeep().remove(neighbor);
            extended.toDelete().add(neighbor);
          }
        }
      }
    }
    return extended;
  }

  private void export(GridClassification classified, Grid grid) {
    int i = 0;
    for (var entry : grid.data().entrySet()) {
      var idx = entry.getKey();
      var geometry = entry.getValue();
      var subExporter = exporter.subSuffix(String.valueOf(++i));

      if (geometry instanceof Polygon polygon) {
        if (classified.toKeep().contains(idx)) {
          subExporter.export(SKINNY_ARM_TO_KEEP_CELL_POLYGON, polygon);
        } else {
          subExporter.export(SKINNY_ARM_TO_DELETE_CELL_POLYGON, polygon);
        }

        if (classified.invalidGrid().contains(idx)) {
          subExporter.export(SKINNY_ARM_INVALID_CELL_POLYGON, polygon);
        } else {
          subExporter.export(SKINNY_ARM_VALID_CELL_POLYGON, polygon);
        }
      }
    }
  }

  private List<Set<CellIndex>> getConnected(Set<CellIndex> cells) {
    List<Set<CellIndex>> groups = new ArrayList<>();
    Set<CellIndex> visited = new HashSet<>();

    for (var cell : cells) {
      if (visited.contains(cell)) continue;

      Set<CellIndex> group = new HashSet<>();
      Deque<CellIndex> stack = new ArrayDeque<>();
      stack.push(cell);

      while (!stack.isEmpty()) {
        var current = stack.pop();
        if (!visited.add(current)) continue;

        group.add(current);

        for (int dx = -1; dx <= 1; dx++) {
          for (int dy = -1; dy <= 1; dy++) {
            if (dx == 0 && dy == 0) continue;

            var neighbor = new CellIndex(current.x() + dx, current.y() + dy);
            if (cells.contains(neighbor) && !visited.contains(neighbor)) {
              stack.push(neighbor);
            }
          }
        }
      }

      groups.add(group);
    }

    return groups;
  }

  private double getActualMaxWidth(Polygon polygon) {
    if (polygon == null || polygon.isEmpty()) return 0.0;
    var mic = new MaximumInscribedCircle(polygon, 0.1);
    return mic.getRadiusLine().getLength() * 2.0;
  }

  private boolean isSkinny(Geometry geometry) {
    if (!(geometry instanceof Polygon polygon)) return false;
    double width = getActualMaxWidth(polygon);
    return width <= conf.maxWidthWithoutExtended();
  }

  private boolean isSkinnyArm(Geometry geometry) {
    if (!(geometry instanceof Polygon polygon)) return false;

    var obb = new OBB2DComputer().apply(polygon);
    double height = Math.max(obb.width(), obb.height());
    double width = getActualMaxWidth(polygon);
    return height >= conf.minHeight() && width <= conf.maxWidth();
  }

  private static Geometry getGeometry(Collection<CellIndex> idx, Grid grid) {
    var geometries = idx.stream().map(grid::get).toList();
    var merged = GeometryCombiner.combine(geometries);
    return merged.buffer(0);
  }

  private static Polygon getLargestPolygon(Geometry geometry) {
    if (geometry instanceof Polygon polygon) return polygon;
    if (geometry instanceof MultiPolygon multi) {
      double maxArea = -1;
      Polygon best = null;

      for (int i = 0; i < multi.getNumGeometries(); i++) {
        var polygon = (Polygon) multi.getGeometryN(i);
        double area = polygon.getArea();

        if (area > maxArea) {
          maxArea = area;
          best = polygon;
        }
      }

      return best;
    }
    throw new RuntimeException("Geometry Type Not Supported");
  }

  private static Polygon createCellPolygon(int gx, int gy, double size) {
    var coordinates =
        new Coordinate[] {
          new Coordinate(gx * size, gy * size),
          new Coordinate((gx + 1) * size, gy * size),
          new Coordinate((gx + 1) * size, (gy + 1) * size),
          new Coordinate(gx * size, (gy + 1) * size),
          new Coordinate(gx * size, gy * size)
        };

    return geometryFactory.createPolygon(coordinates);
  }

  private record CellIndex(int x, int y) {}

  @Builder(toBuilder = true)
  private record GridClassification(
      Set<CellIndex> toKeep, Set<CellIndex> invalidGrid, Set<CellIndex> toDelete) {

    public GridClassification copy() {
      return this.toBuilder().build();
    }
  }

  private record Grid(Map<CellIndex, Geometry> data) {
    Geometry get(CellIndex idx) {
      return data.get(idx);
    }

    void put(CellIndex idx, Geometry value) {
      data.put(idx, value);
    }
  }

  @Builder(toBuilder = true)
  public record PolygonSkinnyArmRemoverConf(
      double maxWidthWithoutExtended,
      double maxWidth,
      double minHeight,
      double gridSize,
      double cellMin2DArea,
      double minAreaToCheck,
      int cellMinNeighborsCount) {}
}
