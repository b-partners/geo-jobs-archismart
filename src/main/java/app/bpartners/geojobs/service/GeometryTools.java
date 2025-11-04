package app.bpartners.geojobs.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class GeometryTools {

  public int getMinimumEnclosingRadius(List<List<BigDecimal>> points) {
    List<List<BigDecimal>> shuffled = new ArrayList<>(points);
    Collections.shuffle(shuffled, new Random());
    return (int) Math.ceil(welzl(shuffled, new ArrayList<>(), shuffled.size()));
  }

  private double welzl(List<List<BigDecimal>> points, List<List<BigDecimal>> boundary, int n) {
    if (n == 0 || boundary.size() == 3) {
      return computeRadius(boundary);
    }

    List<BigDecimal> p = points.get(n - 1);
    double radius = welzl(points, boundary, n - 1);

    if (isInsideCircle(boundary, p, radius)) {
      return radius;
    }

    boundary.add(p);
    double result = welzl(points, boundary, n - 1);
    boundary.removeLast();
    return result;
  }

  private boolean isInsideCircle(
      List<List<BigDecimal>> boundary, List<BigDecimal> p, double radius) {
    double[] center = computeCenter(boundary);
    if (center == null) return false;
    return distance(center[0], center[1], p) <= radius + 1e-10;
  }

  private double computeRadius(List<List<BigDecimal>> boundary) {
    double[] center = computeCenter(boundary);
    if (center == null) return 0.0;

    double maxDist = 0.0;
    for (List<BigDecimal> p : boundary) {
      maxDist = Math.max(maxDist, distance(center[0], center[1], p));
    }
    return maxDist;
  }

  private double distance(double cx, double cy, List<BigDecimal> point) {
    double dx = cx - point.getFirst().doubleValue();
    double dy = cy - point.get(1).doubleValue();
    return Math.sqrt(dx * dx + dy * dy);
  }

  private double[] computeCenter(List<List<BigDecimal>> boundary) {
    if (boundary.isEmpty()) return null;

    if (boundary.size() == 1) {
      return new double[] {
        boundary.getFirst().getFirst().doubleValue(), boundary.getFirst().get(1).doubleValue()
      };
    }

    if (boundary.size() == 2) {
      double x1 = boundary.getFirst().getFirst().doubleValue();
      double y1 = boundary.getFirst().get(1).doubleValue();
      double x2 = boundary.get(1).getFirst().doubleValue();
      double y2 = boundary.get(1).get(1).doubleValue();
      return new double[] {(x1 + x2) / 2, (y1 + y2) / 2};
    }

    double x1 = boundary.getFirst().getFirst().doubleValue();
    double y1 = boundary.getFirst().get(1).doubleValue();
    double x2 = boundary.get(1).getFirst().doubleValue();
    double y2 = boundary.get(1).get(1).doubleValue();
    double x3 = boundary.get(2).getFirst().doubleValue();
    double y3 = boundary.get(2).get(1).doubleValue();

    double d = 2 * (x1 * (y2 - y3) + x2 * (y3 - y1) + x3 * (y1 - y2));
    if (d == 0) return null;

    double ux =
        ((x1 * x1 + y1 * y1) * (y2 - y3)
                + (x2 * x2 + y2 * y2) * (y3 - y1)
                + (x3 * x3 + y3 * y3) * (y1 - y2))
            / d;
    double uy =
        ((x1 * x1 + y1 * y1) * (x3 - x2)
                + (x2 * x2 + y2 * y2) * (x1 - x3)
                + (x3 * x3 + y3 * y3) * (x2 - x1))
            / d;

    return new double[] {ux, uy};
  }
}
