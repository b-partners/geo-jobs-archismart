package app.bpartners.geojobs.model.lidar.planes.topology;

import static app.bpartners.geojobs.model.lidar.planes.topology.RupturePointsComputer.getORuptures;
import static app.bpartners.geojobs.model.lidar.planes.topology.model.RoofRelationType.S;

import app.bpartners.geojobs.model.lidar.LasPointGeometry;
import app.bpartners.geojobs.model.lidar.planes.Plane3D;
import app.bpartners.geojobs.model.lidar.planes.topology.model.RoofTopology;
import app.bpartners.geojobs.model.lidar.planes.topology.model.Rupture;
import java.util.List;
import java.util.function.BiConsumer;
import org.locationtech.jts.geom.Coordinate;

public class RupturePointsSnappingComputer implements BiConsumer<List<Plane3D>, RoofTopology> {
  @Override
  public void accept(List<Plane3D> planes, RoofTopology topology) {
    int n = planes.size();
    for (int i = 0; i < n; i++) {
      for (int j = i + 1; j < n; j++) {
        var relation = topology.getRelations()[i][j];
        if (!S.equals(relation)) continue;

        updateORuptureStart(i, j, n, topology);
        updateORuptureStart(j, i, n, topology);
      }
    }
  }

  void updateORuptureStart(int i, int j, int n, RoofTopology topology) {
    var sRupture = topology.getRuptures()[i][j];
    var oRuptures = getORuptures(i, n, topology);

    for (var o : oRuptures) {
      var x = o.getPlaneAIndex();
      var y = o.getPlaneBIndex();
      var toAdd = getStartCoordinate(sRupture.getStart(), sRupture.getEnd(), o);
      topology.getRuptures()[x][y].getStartIntersection().add(toAdd);
      topology.getRuptures()[y][x].getStartIntersection().add(toAdd);
    }
  }

  private Coordinate getStartCoordinate(Coordinate sStart, Coordinate sEnd, Rupture o) {
    var startDistance = o.getLine().distance(new LasPointGeometry(sStart));
    var endDistance = o.getLine().distance(new LasPointGeometry(sEnd));
    return startDistance < endDistance ? sStart : sEnd;
  }
}
