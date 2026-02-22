package app.bpartners.geojobs.model.lidar.planes;

import static java.util.function.Predicate.not;
import static java.util.stream.Collectors.toSet;

import app.bpartners.geojobs.model.lidar.LasPointGeometry;
import app.bpartners.geojobs.model.lidar.planes.algorithm.XYZPointsCluster;
import app.bpartners.geojobs.model.lidar.planes.exporter.Plane3DExtractionStepExporter;
import java.security.SecureRandom;
import java.util.*;
import java.util.function.Function;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class OnePlane3DExtractor
    implements Function<Set<LasPointGeometry>, OnePlane3DExtractor.Result> {
  private final Plane3DExtractorConf conf;
  private final XYZPointsCluster xyzPointsCluster;
  private final Plane3DExtractionStepExporter exporter;

  public OnePlane3DExtractor(Plane3DExtractorConf conf, Plane3DExtractionStepExporter exporter) {
    this(
        conf,
        new XYZPointsCluster(conf.planeExtractionConf().pointContinuationThreshold()),
        exporter);
  }

  @Override
  public Result apply(Set<LasPointGeometry> points) {
    if (points.size() < 3) {
      throw new IllegalArgumentException("At least 3 points are required.");
    }

    List<LasPointGeometry> bestInliers = new ArrayList<>();
    Plane3D bestModel = null;

    var random = new SecureRandom();

    var boxConf = conf.boxConf();
    var kernelConf = conf.kernelConf();
    var planeExtractionConf = conf.planeExtractionConf();
    var planeDelimitationConf = conf.planeDelimitationConf();
    var kernelValueConf =
        Kernel.Conf.builder()
            .attempts(kernelConf.attempts())
            .maxLength(kernelConf.maxLength())
            .squaredThreshold(kernelConf.threshold() * kernelConf.threshold())
            .degEpsilon(kernelConf.degEpsilon())
            .minVectorNorm(kernelConf.minVectorNorm())
            .build();

    for (int i = 0; i < planeExtractionConf.iteration(); i++) {
      var optionalKernel = Kernel.from(points, kernelValueConf, random);
      if (optionalKernel.isEmpty()) continue;

      var kernel = optionalKernel.get();
      if (bestModel != null && kernel.size() < 2) continue;

      // --- 2. Fit plane
      var box =
          new Box(
              kernel,
              boxConf.threshold(),
              planeDelimitationConf.concaveRatio(),
              planeDelimitationConf.simplificationEpsilon(),
              exporter);

      // --- 3. Compute distances
      box.add(points);
      var inliers = box.getPoints();

      // --- 4. Keep best
      if (inliers.size() < bestInliers.size()) {
        continue;
      }

      var clusterResult = getBestXYZCluster(inliers);
      if (clusterResult.size() > bestInliers.size()) {
        bestModel = box.getPlane();
        bestInliers = new ArrayList<>(clusterResult);
      }
    }

    if (bestInliers.isEmpty()) {
      return new Result(Plane3D.empty(), points);
    }

    // --- 5. Compute outliers
    var inlierSet = new HashSet<>(bestInliers);
    var outliers = points.stream().filter(not(inlierSet::contains)).collect(toSet());
    return new Result(bestModel.with(inlierSet), outliers);
  }

  private List<LasPointGeometry> getBestXYZCluster(Collection<LasPointGeometry> points) {
    var clusters = xyzPointsCluster.apply(points);
    return clusters.stream().max(Comparator.comparingInt(List::size)).orElseThrow();
  }

  public record Result(Plane3D plane, Set<LasPointGeometry> outliers) {}
}
