package app.bpartners.geojobs.model.lidar.planes;

import app.bpartners.geojobs.model.lidar.LasPointGeometry;
import app.bpartners.geojobs.model.lidar.planes.algorithm.PlaneFitter;
import java.util.*;
import lombok.Builder;
import lombok.Getter;
import org.locationtech.jts.math.Vector2D;

public class Box {
  @Getter private Plane3D plane;
  @Getter private final BoxConf conf;
  @Getter private final Kernel kernel;
  @Getter private boolean didInfiniteGrow;
  @Getter private final List<LasPointGeometry> points;
  @Getter private final boolean doInfiniteGrow;

  private final OBB obb;
  private Set<ExpansionDirection> expansionDirections;

  public Box(BoxConf conf, Kernel kernel, boolean doInfiniteGrow) {
    this.conf = conf;
    this.kernel = kernel;

    this.obb = new OBB();
    this.didInfiniteGrow = false;
    this.doInfiniteGrow = doInfiniteGrow;
    this.plane = PlaneFitter.fit(kernel);
    this.points = new ArrayList<>(plane.getPoints());
    this.expansionDirections = EnumSet.allOf(ExpansionDirection.class);

    updateOBB(this.points);
  }

  public Box(BoxConf conf, Kernel kernel) {
    this(conf, kernel, false);
  }

  public void doInfiniteGrow(Collection<LasPointGeometry> candidates) {
    var filtered =
        candidates.stream().filter(point -> this.plane.distance(point) <= conf.height()).toList();
    this.stopExpansions();
    this.points.addAll(filtered);
    this.didInfiniteGrow = true;
  }

  public boolean shouldDoInfiniteGrow(Collection<LasPointGeometry> candidates) {
    if (doInfiniteGrow) return true;
    return this.points.size() > conf.maxRefitPoints() && candidates.size() > conf.maxRefitPoints();
  }

  public void grow(Collection<LasPointGeometry> candidates) {
    var pointsToProcess = new HashSet<>(candidates);
    while (!pointsToProcess.isEmpty() && canExtend()) {
      if (shouldDoInfiniteGrow(pointsToProcess)) {
        doInfiniteGrow(pointsToProcess);
        break;
      }

      double searchMinU = obb.minU - extension(ExpansionDirection.MIN_U);
      double searchMaxU = obb.maxU + extension(ExpansionDirection.MAX_U);
      double searchMinV = obb.minV - extension(ExpansionDirection.MIN_V);
      double searchMaxV = obb.maxV + extension(ExpansionDirection.MAX_V);

      List<LasPointGeometry> found = new ArrayList<>();
      for (var p : pointsToProcess) {
        if (plane.distance(p) > conf.height()) continue;

        var uv = plane.projectToLocal(p);
        if (uv.getX() >= searchMinU
            && uv.getX() <= searchMaxU
            && uv.getY() >= searchMinV
            && uv.getY() <= searchMaxV) {
          found.add(p);
        }
      }

      if (found.isEmpty()) {
        stopExpansions();
        break;
      }

      this.points.addAll(found);
      found.forEach(pointsToProcess::remove);

      refitPlane();
      updateExpansionDirections(found);
      updateOBB(found);
    }
  }

  private void refitPlane() {
    this.plane = PlaneFitter.fit(points).toBuilder().kernel(kernel).build();
  }

  private void updateExpansionDirections(Collection<LasPointGeometry> candidates) {
    var foundedDirections = new HashSet<ExpansionDirection>();
    int expansionsLength = ExpansionDirection.values().length;
    for (var p : candidates) {
      var uv = this.plane.projectToLocal(p);

      if (uv.getX() < obb.minU) foundedDirections.add(ExpansionDirection.MIN_U);
      if (uv.getX() > obb.maxU) foundedDirections.add(ExpansionDirection.MAX_U);
      if (uv.getY() < obb.minV) foundedDirections.add(ExpansionDirection.MIN_V);
      if (uv.getY() > obb.maxV) foundedDirections.add(ExpansionDirection.MAX_V);

      if (foundedDirections.size() >= expansionsLength) {
        break;
      }
    }
    this.expansionDirections = foundedDirections;
  }

  private void updateOBB(Collection<LasPointGeometry> newPoints) {
    for (var p : newPoints) {
      var uv = this.plane.projectToLocal(p);
      this.obb.update(uv);
    }
  }

  private void stopExpansions() {
    this.expansionDirections.clear();
  }

  private boolean canExtend() {
    return !expansionDirections.isEmpty();
  }

  private double extension(ExpansionDirection direction) {
    return canExtend(direction) ? conf.expansionSize() : 0;
  }

  private boolean canExtend(ExpansionDirection direction) {
    return this.expansionDirections.contains(direction);
  }

  private enum ExpansionDirection {
    MIN_U,
    MAX_U,
    MIN_V,
    MAX_V
  }

  private static class OBB {
    double minU = Double.POSITIVE_INFINITY;
    double maxU = Double.NEGATIVE_INFINITY;
    double minV = Double.POSITIVE_INFINITY;
    double maxV = Double.NEGATIVE_INFINITY;

    public void update(Vector2D uv) {
      this.minU = Math.min(minU, uv.getX());
      this.maxU = Math.max(maxU, uv.getX());
      this.minV = Math.min(minV, uv.getY());
      this.maxV = Math.max(maxV, uv.getY());
    }
  }

  @Builder(toBuilder = true)
  public record BoxConf(double height, double expansionSize, int maxRefitPoints) {}
}
