package app.bpartners.geojobs.model.lidar.planes;

import app.bpartners.geojobs.model.lidar.LasPointGeometry;
import app.bpartners.geojobs.model.lidar.planes.exporter.Plane3DExtractionStepExporter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class Box {
  private final Kernel kernel;
  private final Plane3D plane;
  private final double threshold;
  private final List<LasPointGeometry> points;

  public Box(
      Kernel kernel,
      double threshold,
      double planeDelimitationConcaveRatio,
      double planeDelimitationSimplificationEpsilon,
      Plane3DExtractionStepExporter exporter) {
    this.kernel = kernel;
    this.threshold = threshold;
    this.points = new ArrayList<>(kernel.getChains().getPoints());
    this.plane =
        Plane3D.fit(
            kernel,
            planeDelimitationConcaveRatio,
            planeDelimitationSimplificationEpsilon,
            exporter);
  }

  public boolean contains(LasPointGeometry point) {
    return plane.distance(point) <= threshold;
  }

  public List<LasPointGeometry> add(Collection<LasPointGeometry> points) {
    var insideBox = points.stream().filter(this::contains).toList();

    this.points.addAll(insideBox);
    return insideBox;
  }
}
