package app.bpartners.geojobs.model.lidar.planes.algorithm.model;

import lombok.Getter;

@Getter
public class UnionFind {
  private final int[] parent;
  private final int[] size;

  public UnionFind(int n) {
    parent = new int[n];
    size = new int[n];
    for (int i = 0; i < n; i++) {
      parent[i] = i;
      size[i] = 1;
    }
  }

  public int find(int x) {
    if (parent[x] != x) {
      parent[x] = find(parent[x]);
    }
    return parent[x];
  }

  public void union(int a, int b) {
    a = find(a);
    b = find(b);

    if (a == b) {
      return;
    }

    if (size[a] < size[b]) {
      parent[a] = b;
      size[b] += size[a];
    } else {
      parent[b] = a;
      size[a] += size[b];
    }
  }
}
