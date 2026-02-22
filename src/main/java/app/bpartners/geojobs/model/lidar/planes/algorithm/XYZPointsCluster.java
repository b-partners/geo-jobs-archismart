package app.bpartners.geojobs.model.lidar.planes.algorithm;

import app.bpartners.geojobs.model.lidar.LasPointGeometry;
import app.bpartners.geojobs.model.lidar.planes.algorithm.model.UnionFind;
import java.util.*;
import java.util.function.Function;

public class XYZPointsCluster
    implements Function<Collection<LasPointGeometry>, List<List<LasPointGeometry>>> {
  private final double radius;
  private final double squaredRadius;

  public XYZPointsCluster(double radius) {
    this.radius = radius;
    this.squaredRadius = radius * radius;
  }

  @Override
  public List<List<LasPointGeometry>> apply(Collection<LasPointGeometry> points) {
    var size = points.size();
    if (size == 0) {
      return List.of();
    }

    var list = new ArrayList<>(points);

    var grid = createGrid(list);
    var uf = getConnectConnectedPoints(size, grid, list);
    return getClusterResult(size, list, uf);
  }

  private Map<CellIndex, List<Integer>> createGrid(List<LasPointGeometry> points) {
    Map<CellIndex, List<Integer>> grid = new HashMap<>();
    for (int i = 0; i < points.size(); i++) {
      var p = points.get(i);
      int ix = (int) Math.floor(p.getX() / radius);
      int iy = (int) Math.floor(p.getY() / radius);
      int iz = (int) Math.floor(p.getZ() / radius);
      grid.computeIfAbsent(new CellIndex(ix, iy, iz), k -> new ArrayList<>()).add(i);
    }
    return grid;
  }

  private UnionFind getConnectConnectedPoints(
      int size, Map<CellIndex, List<Integer>> grid, List<LasPointGeometry> points) {
    var unionFind = new UnionFind(size);

    for (int i = 0; i < size; i++) {
      var p = points.get(i);
      int ix = (int) Math.floor(p.getX() / radius);
      int iy = (int) Math.floor(p.getY() / radius);
      int iz = (int) Math.floor(p.getZ() / radius);

      for (int dx = -1; dx <= 1; dx++) {
        for (int dy = -1; dy <= 1; dy++) {
          for (int dz = -1; dz <= 1; dz++) {
            var cell = new CellIndex(ix + dx, iy + dy, iz + dz);
            var neigh = grid.get(cell);
            if (neigh == null) {
              continue;
            }

            var left = points.get(i);
            for (int j : neigh) {
              if (j <= i) {
                continue;
              }

              if (left.squaredDistance(points.get(j)) <= squaredRadius) {
                unionFind.union(i, j);
              }
            }
          }
        }
      }
    }

    return unionFind;
  }

  private List<List<LasPointGeometry>> getClusterResult(
      int size, List<LasPointGeometry> points, UnionFind uf) {
    Map<Integer, List<LasPointGeometry>> clusters = new HashMap<>();

    for (int i = 0; i < size; i++) {
      int root = uf.find(i);
      clusters.computeIfAbsent(root, k -> new ArrayList<>()).add(points.get(i));
    }

    return clusters.values().stream().toList();
  }

  private record CellIndex(int x, int y, int z) {}
}
