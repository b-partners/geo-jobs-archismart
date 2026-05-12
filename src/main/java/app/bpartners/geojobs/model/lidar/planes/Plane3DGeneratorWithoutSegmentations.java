package app.bpartners.geojobs.model.lidar.planes;

import static app.bpartners.geojobs.model.lidar.planes.algorithm.GeometryUtilities.project;

import app.bpartners.geojobs.model.lidar.planes.conf.Plane3DExtractorConf;
import app.bpartners.geojobs.model.lidar.planes.model.DelimitedRoofPoints;
import app.bpartners.geojobs.model.lidar.planes.model.DelimitedRoofPointsItem;
import app.bpartners.geojobs.model.lidar.planes.postprocessing.RoofFaceToLidarAlignmentFixer;
import app.bpartners.geojobs.model.lidar.planes.postprocessing.Snapping2DComputer;
import app.bpartners.geojobs.model.lidar.planes.postprocessing.Snapping3DComputer;
import java.util.*;
import java.util.function.Function;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class Plane3DGeneratorWithoutSegmentations
    implements Function<DelimitedRoofPoints, List<Plane3D>> {
  private final RoofFaceToLidarAlignmentFixer alignmentFixer;
  private final Snapping2DComputer snapping2DComputer;
  private final Snapping3DComputer snapping3DComputer;

  public Plane3DGeneratorWithoutSegmentations(Plane3DExtractorConf conf) {
    this.alignmentFixer = new RoofFaceToLidarAlignmentFixer(conf);
    this.snapping2DComputer = new Snapping2DComputer(1);
    this.snapping3DComputer = new Snapping3DComputer(1.5);
  }

  public Plane3D apply(DelimitedRoofPointsItem item) {
    var plane = getBestPlane(item);
    var delimitation = project(plane, item.getPolygon());
    return plane.toBuilder().delimitation(delimitation).build();
  }

  @Override
  public List<Plane3D> apply(DelimitedRoofPoints delimitedPoints) {
    var rawPlanes = Arrays.stream(delimitedPoints.getItems()).map(this::apply).toList();
    return postProcess(delimitedPoints, rawPlanes);
  }

  private List<Plane3D> postProcess(
      DelimitedRoofPoints delimitedRoofPoints, List<Plane3D> rawPlanes) {
    var postProcessed = this.alignmentFixer.apply(delimitedRoofPoints, rawPlanes);
    postProcessed = this.snapping2DComputer.apply(postProcessed);
    return this.snapping3DComputer.apply(postProcessed);
  }

  private Plane3D getBestPlane(DelimitedRoofPointsItem item) {
    var conf = Plane3DExtractorConf.getDefault().toBuilder().doSkinnyArmRemover(false).build();
    var extractor = new OnePlane3DExtractor(conf);
    return extractor.apply(item.getPoints(), null).plane();
  }
}
