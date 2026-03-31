package app.bpartners.geojobs.model.lidar.planes.postprocessing;

import static app.bpartners.geojobs.model.lidar.planes.algorithm.PointsDelimitationComputer.getConcave;

import app.bpartners.geojobs.model.lidar.LasPointGeometry;
import app.bpartners.geojobs.model.lidar.planes.PlaneDelimitation;
import app.bpartners.geojobs.model.lidar.planes.exporter.Plane3DExtractionStepExporter;
import app.bpartners.geojobs.model.lidar.planes.postprocessing.PolygonSkinnyArmRemover.PolygonSkinnyArmRemoverConf;
import java.util.*;
import java.util.function.BiFunction;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class SkinnyArmPointFilter
    implements BiFunction<
        Collection<LasPointGeometry>, Plane3DExtractionStepExporter, List<LasPointGeometry>> {
  private final PolygonSkinnyArmRemoverConf polygonSkinnyArmRemoverConf;
  private final PlaneDelimitation.PlaneDelimitationConf delimitationConf;

  @Override
  public List<LasPointGeometry> apply(
      Collection<LasPointGeometry> points, Plane3DExtractionStepExporter exporter) {
    var concave = getConcave(points, delimitationConf.concaveRatio().getValue(points.size()));
    var skinnyArmRemover = new PolygonSkinnyArmRemover(polygonSkinnyArmRemoverConf, exporter);
    var withoutSkinnyArm = skinnyArmRemover.apply(concave);

    if (concave == withoutSkinnyArm) {
      return new ArrayList<>(points);
    }

    List<LasPointGeometry> inliers = new ArrayList<>();
    for (var point : points) {
      if (withoutSkinnyArm.intersects(point) || withoutSkinnyArm.contains(point)) {
        inliers.add(point);
      }
    }

    return inliers;
  }
}
