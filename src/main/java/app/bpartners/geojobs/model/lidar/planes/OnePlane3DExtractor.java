package app.bpartners.geojobs.model.lidar.planes;

import static app.bpartners.geojobs.model.lidar.planes.exporter.Plane3DExtractionStep.*;
import static java.util.function.Predicate.not;
import static java.util.stream.Collectors.toSet;

import app.bpartners.geojobs.model.lidar.LasPointGeometry;
import app.bpartners.geojobs.model.lidar.planes.algorithm.XYZPointsCluster;
import app.bpartners.geojobs.model.lidar.planes.conf.Plane3DExtractorConf;
import app.bpartners.geojobs.model.lidar.planes.exporter.Plane3DExtractionStepExporter;
import app.bpartners.geojobs.model.lidar.planes.postprocessing.SkinnyArmPointFilter;
import java.security.SecureRandom;
import java.util.*;
import java.util.function.BiFunction;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class OnePlane3DExtractor
    implements BiFunction<
        Set<LasPointGeometry>, Plane3DExtractionStepExporter, OnePlane3DExtractor.Result> {
  private final Plane3DExtractorConf conf;
  private final XYZPointsCluster xyzPointsCluster;
  private final SkinnyArmPointFilter skinnyArmPointFilter;

  public OnePlane3DExtractor(Plane3DExtractorConf conf) {
    this.conf = conf;
    this.xyzPointsCluster =
        new XYZPointsCluster(conf.planeExtractionConf().pointContinuationThreshold());
    this.skinnyArmPointFilter =
        new SkinnyArmPointFilter(conf.polygonSkinnyArmRemoverConf(), conf.planeDelimitationConf());
  }

  public Result apply(
      Kernel kernel, Set<LasPointGeometry> points, Plane3DExtractionStepExporter exporter) {
    if (points.isEmpty()) {
      return new Result(Plane3D.empty(), points);
    }

    var box = new Box(conf.boxConf(), kernel, false);
    box.grow(points);

    var inliers = box.getPoints();

    var clusterResult = getBestXYZCluster(inliers);
    var finalInliers = clusterResult;
    if (conf.doSkinnyArmRemover()) {
      finalInliers = this.skinnyArmPointFilter.apply(clusterResult, exporter);
    }

    var inlierSet = new HashSet<>(finalInliers);
    var outliers = points.stream().filter(not(inlierSet::contains)).collect(toSet());

    var plane =
        box.getPlane().toBuilder()
            .points(inlierSet)
            .delimitationConf(conf.planeDelimitationConf())
            .build();

    return new Result(plane, outliers);
  }

  @Override
  public Result apply(Set<LasPointGeometry> points, Plane3DExtractionStepExporter exporter) {
    if (points.size() < 3) {
      throw new IllegalArgumentException("At least 3 points are required.");
    }

    List<LasPointGeometry> bestInliers = new ArrayList<>();
    Plane3D bestModel = null;

    var random = new SecureRandom();

    var boxConf = conf.boxConf();
    var kernelConf = conf.kernelConf();
    var planeExtractionConf = conf.planeExtractionConf();
    var delimitationConf = conf.planeDelimitationConf();

    for (int i = 0; i < planeExtractionConf.iteration(); i++) {
      var optionalKernel = Kernel.from(points, kernelConf, random);
      if (optionalKernel.isEmpty()) continue;

      var kernel = optionalKernel.get();
      if (bestModel != null && kernel.size() < 2) continue;

      var box = new Box(boxConf, kernel);
      box.grow(points);
      var inliers = box.getPoints();

      if (inliers.size() < bestInliers.size()) {
        continue;
      }

      var plane = box.getPlane();
      if (isNotValid(plane)) continue;

      if (!box.isDidInfiniteGrow()) {
        bestModel = box.getPlane();
        bestInliers = new ArrayList<>(inliers);
        continue;
      }

      if (!conf.doXYZClustering()) {
        if (inliers.size() > bestInliers.size()) {
          bestModel = box.getPlane();
          bestInliers = inliers;
        }
        continue;
      }

      var clusterResult = getBestXYZCluster(inliers);
      if (clusterResult.size() > bestInliers.size()) {
        bestModel = box.getPlane();
        bestInliers = clusterResult;
      }
    }

    if (bestInliers.isEmpty()) {
      return new Result(Plane3D.empty(), points);
    }

    // --- 5. Compute outliers
    assert bestModel != null;

    var finalInliers = bestInliers;
    if (conf.doSkinnyArmRemover()) {
      finalInliers = this.skinnyArmPointFilter.apply(bestInliers, exporter);
    }

    if (exporter != null) {
      exporter.export(RAW_PLANE_EXTRACTION, bestInliers);
      exporter.export(RAW_PLANE_KERNEL, bestModel.getKernel().getChains().getPoints());
    }

    var inlierSet = new HashSet<>(finalInliers);
    var outliers = points.stream().filter(not(inlierSet::contains)).collect(toSet());
    var plane = bestModel.toBuilder().points(inlierSet).delimitationConf(delimitationConf).build();
    return new Result(plane, outliers);
  }

  private List<LasPointGeometry> getBestXYZCluster(Collection<LasPointGeometry> points) {
    var clusters = xyzPointsCluster.apply(points);
    return clusters.stream().max(Comparator.comparingInt(List::size)).orElseThrow();
  }

  private boolean isNotValid(Plane3D plane) {
    var maxSlope = conf.planeConf().maxSlope();
    if (maxSlope <= 0) {
      log.warn("Plane3DExtractorConf.PlaneConf.maxSlope is not set");
      return false;
    }
    return plane.getSlopeInDegrees().getValue() > conf.planeConf().maxSlope();
  }

  public record Result(Plane3D plane, Set<LasPointGeometry> outliers) {}
}
