package app.bpartners.geojobs.model.lidar.planes.topology;

import static app.bpartners.geojobs.model.lidar.planes.algorithm.GeometryUtilities.extend;
import static app.bpartners.geojobs.model.lidar.planes.topology.model.RoofRelationType.*;
import static java.util.Comparator.comparingDouble;

import app.bpartners.geojobs.model.lidar.LasPointGeometry;
import app.bpartners.geojobs.model.lidar.planes.Plane3D;
import app.bpartners.geojobs.model.lidar.planes.topology.model.RoofTopology;
import app.bpartners.geojobs.model.lidar.planes.topology.model.Rupture;
import java.util.*;
import java.util.function.BiConsumer;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.linearref.LengthIndexedLine;

public class RupturePointsComputer implements BiConsumer<List<Plane3D>, RoofTopology> {
  private static final double EXTENSION = 20;

  @Override
  public void accept(List<Plane3D> planes, RoofTopology topology) {
    computeIntersectionRupturePoints(planes, topology);
  }

  private static void computeIntersectionRupturePoints(
      List<Plane3D> planes, RoofTopology topology) {
    int n = planes.size();
    for (int i = 0; i < n; i++) {
      for (int j = i + 1; j < n; j++) {
        var relation = topology.getRelations()[i][j];
        if (!S.equals(relation)) continue;

        computeSAndOIntersection(i, j, planes, topology);
        computeSAndOIntersection(j, i, planes, topology);
      }
    }
  }

  private static void computeSAndOIntersection(
      int i, int j, List<Plane3D> planes, RoofTopology topology) {
    var oRuptures = getORuptures(i, planes.size(), topology);
    if (oRuptures.isEmpty()) return;

    var sRupture = topology.getRuptures()[i][j];
    var extendedSLine = extend(sRupture.getLine(), EXTENSION);
    var indexedSLine = new LengthIndexedLine(extendedSLine);

    for (var o : oRuptures) {
      var extendedOLine = extend(o.getLine(), EXTENSION);
      if (!extendedSLine.intersects(extendedOLine)) continue;

      var intersection = extendedSLine.intersection(extendedOLine);
      if (!(intersection instanceof Point pointIntersection)) continue;

      var intersectionCoordinate = pointIntersection.getCoordinate();
      var intersectionLength = indexedSLine.project(intersectionCoordinate);
      var bCoordinate = getBCoordinate(sRupture.getLine(), o.getLine());
      var bPrimeLength = indexedSLine.project(bCoordinate);

      var intersectionWithZ = compute3DIntersection(intersectionCoordinate, planes.get(i));
      var aRupture = topology.getRuptures()[i][j];
      var bRupture = topology.getRuptures()[j][i];

      var isStartIntersection = shouldAddInStart(bPrimeLength, intersectionLength);
      addIntersectionToRupture(aRupture, bRupture, intersectionWithZ, isStartIntersection);
      addBCoordinateToORupture(bCoordinate, o, topology, planes.get(i));
    }
  }

  private static void addIntersectionToRupture(
      Rupture a, Rupture b, Coordinate intersection, boolean isStart) {
    if (isStart) {
      a.getStartIntersection().add(intersection);
      b.getStartIntersection().add(intersection);
    } else {
      a.getEndIntersection().add(intersection);
      b.getEndIntersection().add(intersection);
    }
  }

  private static void addBCoordinateToORupture(
      Coordinate bCoordinate, Rupture rupture, RoofTopology topology, Plane3D plane) {
    var x = bCoordinate.getX();
    var y = bCoordinate.getY();
    var z = plane.zAt(x, y);
    var bWithZ = new Coordinate(x, y, z);

    var i = rupture.getPlaneAIndex();
    var j = rupture.getPlaneBIndex();
    topology.getRuptures()[i][j].getEndIntersection().add(bWithZ);
    topology.getRuptures()[j][i].getEndIntersection().add(bWithZ);
  }

  private static Coordinate compute3DIntersection(Coordinate intersection, Plane3D plane) {
    var x = intersection.getX();
    var y = intersection.getY();
    var z = plane.zAt(x, y);
    return new Coordinate(x, y, z);
  }

  private static boolean shouldAddInStart(double bPrimeLength, double intersectionLength) {
    /*
     *       CASE_1
     *
     *           b
     *           |\
     *           | \ (O_Line)
     *           |  \
     *    x------b'--a-----x'  : S_Line
     *               ^
     *          intersection
     * ============================================================
     *       CASE_2
     *
     *               b
     *              /|
     *   (O_Line)  / |
     *            /  |
     *    x------a---b'-------x'  : S_Line
     *           ^
     *      intersection
     * */
    return bPrimeLength < intersectionLength;
  }

  private static Coordinate getBCoordinate(LineString sLine, LineString oLine) {
    return Arrays.stream(oLine.getCoordinates())
        .max(comparingDouble(coordinate -> sLine.distance(new LasPointGeometry(coordinate))))
        .orElseThrow();
  }

  static List<Rupture> getORuptures(int i, int n, RoofTopology topology) {
    List<Rupture> ruptures = new ArrayList<>();
    for (int j = 0; j < n; j++) {
      if (i == j) continue;
      var relation = topology.getRelations()[i][j];
      if (!O_PLUS.equals(relation) && !O_MINUS.equals(relation)) continue;
      ruptures.add(topology.getRuptures()[i][j]);
    }
    return ruptures;
  }
}
