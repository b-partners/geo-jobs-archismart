package app.bpartners.geojobs.model.lidar.planes.postprocessing;

import static app.bpartners.geojobs.model.lidar.planes.algorithm.LasPointGeometryUtilities.project;
import static app.bpartners.geojobs.model.lidar.planes.algorithm.PointsDelimitationComputer.getConcave;
import static java.lang.Double.POSITIVE_INFINITY;
import static java.util.Comparator.comparingDouble;

import app.bpartners.geojobs.model.lidar.planes.Plane3D;
import java.util.*;
import java.util.function.UnaryOperator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class Plane3DMerger implements UnaryOperator<Collection<Plane3D>> {
  private final double concaveRatio;
  private final double epsilonSlope;
  private final double epsilonZDistance;
  private final double epsilonXYDistance;

  @Override
  public List<Plane3D> apply(Collection<Plane3D> planes) {
    Set<Plane3D> visited = new HashSet<>();
    List<Plane3D> result = new ArrayList<>();
    List<Plane3D> sorted = getSortedPlanesByArea(planes);

    for (var p1 : sorted) {
      if (visited.contains(p1)) {
        continue;
      }

      var merged = p1;
      for (var p2 : sorted) {
        if (p1 == p2 || visited.contains(p2)) {
          continue;
        }

        if (shouldMerge(merged, p2)) {
          merged = merge(merged, p2, concaveRatio);
          visited.add(p2);
        }
      }

      result.add(merged);
      visited.add(p1);
    }

    return result;
  }

  private boolean shouldMerge(Plane3D p1, Plane3D p2) {
    if (!isParallel(p1, p2)) {
      return false;
    }

    if (!isXYClose(p1, p2)) {
      return false;
    }

    return isZClose(p1, p2);
  }

  private boolean isParallel(Plane3D p1, Plane3D p2) {
    double dot = p1.getA() * p2.getA() + p1.getB() * p2.getB() + p1.getC() * p2.getC();
    double norms = p1.getNorm() * p2.getNorm();

    double cos = Math.abs(dot / norms);
    return cos > Math.cos(Math.toRadians(epsilonSlope));
  }

  private boolean isXYClose(Plane3D plane1, Plane3D plane2) {
    var p1 = plane1.getDelimitation();
    var p2 = plane2.getDelimitation();
    return p1.distance(p2) < epsilonXYDistance;
  }

  private boolean isZClose(Plane3D plane1, Plane3D plane2) {
    var dz =
        Arrays.stream(plane1.getDelimitation().getCoordinates())
            .mapToDouble(
                p1Point -> {
                  var p2ZPoint = plane2.zAt(p1Point.getX(), p1Point.getY());
                  return Math.abs(p1Point.getZ() - p2ZPoint);
                })
            .average()
            .orElse(POSITIVE_INFINITY);

    return dz <= epsilonZDistance;
  }

  public static Plane3D merge(Plane3D p1, Plane3D p2, double concaveRatio) {
    if (p1.getArea() < p2.getArea()) {
      return merge(p2, p1, concaveRatio);
    }

    var points = new ArrayList<>(p1.getPoints());
    points.addAll(project(p2.getPoints(), p1));
    var delimitation3D = getConcave(points, concaveRatio);

    return p1.toBuilder()
        .points(new HashSet<>(points))
        .delimitation(delimitation3D)
        .convexDelimitation(null)
        .build();
  }

  private static List<Plane3D> getSortedPlanesByArea(Collection<Plane3D> planes) {
    return planes.stream().sorted(comparingDouble(Plane3D::get2DArea).reversed()).toList();
  }
}
