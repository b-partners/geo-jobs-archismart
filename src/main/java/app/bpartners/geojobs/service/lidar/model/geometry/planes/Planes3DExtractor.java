package app.bpartners.geojobs.service.lidar.model.geometry.planes;

import app.bpartners.geojobs.service.lidar.model.geometry.LasPointGeometry;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;

public class Planes3DExtractor implements Function<Collection<LasPointGeometry>, List<Plane3D>> {
  private final int minimumPointCount;
  private final OnePlane3DExtractor onePlane3DExtractor;
  private final Plane3DContinuationCluster plane3DContinuationCluster;

  private static final int PLANE_EXTRACTION_ITERATION = 200;

  public Planes3DExtractor(double threshold, double continuationThreshold, int minimumPointCount) {
    this.minimumPointCount = minimumPointCount;
    this.onePlane3DExtractor = new OnePlane3DExtractor(PLANE_EXTRACTION_ITERATION, threshold);
    this.plane3DContinuationCluster =
        new Plane3DContinuationCluster(continuationThreshold, minimumPointCount);
  }

  @Override
  public List<Plane3D> apply(Collection<LasPointGeometry> points) {
    List<Plane3D> planes = new ArrayList<>();
    List<LasPointGeometry> pointsToProcess = new ArrayList<>(points);

    while (pointsToProcess.size() > minimumPointCount) {
      var result = onePlane3DExtractor.apply(pointsToProcess);
      var newPlane = result.plane();

      if (newPlane.getPoints().size() < minimumPointCount) {
        break;
      }

      var clusterResult = plane3DContinuationCluster.apply(newPlane);
      var continuedPlane = clusterResult.plane();
      if (continuedPlane.getPoints().size() < minimumPointCount) {
        break;
      }

      planes.add(continuedPlane);
      pointsToProcess = new ArrayList<>(result.outliers());
      pointsToProcess.addAll(clusterResult.outliers());
    }

    return planes;
  }
}
