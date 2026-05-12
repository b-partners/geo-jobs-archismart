package app.bpartners.geojobs.model.lidar.planes.topology;

import static app.bpartners.geojobs.model.lidar.planes.topology.model.RoofRelationType.NONE;

import app.bpartners.geojobs.model.lidar.planes.Plane3D;
import app.bpartners.geojobs.model.lidar.planes.postprocessing.model.ChimneyPlane3D;
import app.bpartners.geojobs.model.lidar.planes.topology.RoofRelationClassifier.RoofRelationClassifierConf;
import app.bpartners.geojobs.model.lidar.planes.topology.model.RoofRelationType;
import app.bpartners.geojobs.model.lidar.planes.topology.model.RoofTopology;
import app.bpartners.geojobs.model.lidar.planes.topology.model.Rupture;
import java.util.HashSet;
import java.util.List;
import java.util.function.Function;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class RoofTopologyBuilder implements Function<List<Plane3D>, RoofTopology> {
  private static final double MIN_AREA = 8;
  private static final double MIN_SLOPE = 6;
  private static final double CONTAINS_RATIO_THRESHOLD = 1.0 / 3.0;

  private final RuptureComputer ruptureComputer;
  private final RoofRelationClassifier classifier;

  public RoofTopologyBuilder(RoofRelationClassifierConf conf) {
    this.ruptureComputer = new RuptureComputer();
    this.classifier = new RoofRelationClassifier(conf);
  }

  @Override
  public RoofTopology apply(List<Plane3D> planes) {
    var n = planes.size();
    var ruptures = new Rupture[n][n];
    var adjacency = new boolean[n][n];
    var relations = new RoofRelationType[n][n];

    for (int i = 0; i < n; i++) {
      var a = planes.get(i);
      var isAAChimney = a instanceof ChimneyPlane3D;
      for (int j = i + 1; j < n; j++) {
        var b = planes.get(j);
        var isBAChimney = b instanceof ChimneyPlane3D;

        if (isAAChimney || isBAChimney || a.get2DArea() < MIN_AREA || b.get2DArea() < MIN_AREA) {
          empty(i, j, adjacency, relations);
          continue;
        }

        if (a.getSlopeInDegrees().getValue() < MIN_SLOPE
            || b.getSlopeInDegrees().getValue() < MIN_SLOPE) {
          empty(i, j, adjacency, relations);
          continue;
        }

        if (isOverlappingTooMuch(a, b)) {
          empty(i, j, adjacency, relations);
          continue;
        }

        var relation = this.classifier.apply(a, b);
        if (NONE.equals(relation)) {
          empty(i, j, adjacency, relations);
          continue;
        }

        var optionalRupture = this.ruptureComputer.apply(a, b);
        if (optionalRupture.isEmpty()) {
          empty(i, j, adjacency, relations);
          continue;
        }

        adjacency[i][j] = true;
        adjacency[j][i] = true;
        relations[i][j] = relation;
        relations[j][i] = relation;

        var rupture = optionalRupture.get();
        ruptures[i][j] = rupture.toBuilder().planeAIndex(i).planeBIndex(j).build();
        ruptures[j][i] =
            rupture.toBuilder()
                .planeAIndex(j)
                .planeBIndex(i)
                .endIntersection(new HashSet<>())
                .startIntersection(new HashSet<>())
                .build();
      }
    }

    return RoofTopology.builder()
        .ruptures(ruptures)
        .adjacency(adjacency)
        .relations(relations)
        .build();
  }

  private boolean isOverlappingTooMuch(Plane3D a, Plane3D b) {
    var polyA = a.getDelimitation();
    var polyB = b.getDelimitation();

    var intersection = polyA.intersection(polyB);
    if (intersection.isEmpty() || intersection.getArea() <= 0) {
      return false;
    }

    if (!intersection.isValid()) intersection = intersection.buffer(0);
    double intersectionArea = intersection.getArea();
    double ratioA = intersectionArea / polyA.getArea();
    double ratioB = intersectionArea / polyB.getArea();

    return ratioA > CONTAINS_RATIO_THRESHOLD || ratioB > CONTAINS_RATIO_THRESHOLD;
  }

  private static void empty(int i, int j, boolean[][] adjacency, RoofRelationType[][] relations) {
    adjacency[i][j] = false;
    adjacency[j][i] = false;
    relations[i][j] = NONE;
    relations[j][i] = NONE;
  }
}
