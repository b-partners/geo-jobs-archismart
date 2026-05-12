package app.bpartners.geojobs.model.lidar.planes.topology;

import app.bpartners.geojobs.model.lidar.planes.Plane3D;
import app.bpartners.geojobs.model.lidar.planes.topology.RoofRelationClassifier.RoofRelationClassifierConf;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;

public class RoofReconstructor implements Function<Collection<Plane3D>, List<Plane3D>> {
  private final RoofDataComputer roofDataComputer;
  private final RoofTopologyBuilder topologyBuilder;
  private final RupturePointsComputer rupturePointsComputer;
  private final RupturePointsSnappingComputer rupturePointsSnappingComputer;

  public RoofReconstructor(RoofRelationClassifierConf conf) {
    this.roofDataComputer = new RoofDataComputer();
    this.topologyBuilder = new RoofTopologyBuilder(conf);
    this.rupturePointsComputer = new RupturePointsComputer();
    this.rupturePointsSnappingComputer = new RupturePointsSnappingComputer();
  }

  @Override
  public List<Plane3D> apply(Collection<Plane3D> planes) {
    var list = new ArrayList<>(planes);
    var topology = this.topologyBuilder.apply(list);

    this.rupturePointsComputer.accept(list, topology);
    this.rupturePointsSnappingComputer.accept(list, topology);
    this.roofDataComputer.accept(list, topology);

    return list;
  }
}
