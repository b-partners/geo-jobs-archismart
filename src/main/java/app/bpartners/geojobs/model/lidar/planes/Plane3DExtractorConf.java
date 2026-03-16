package app.bpartners.geojobs.model.lidar.planes;

import app.bpartners.geojobs.model.lidar.planes.Box.BoxConf;
import app.bpartners.geojobs.model.lidar.planes.Kernel.KernelConf;
import lombok.Builder;

@Builder(toBuilder = true)
public record Plane3DExtractorConf(
    BoxConf boxConf,
    PlaneConf planeConf,
    KernelConf kernelConf,
    ChimneyFixerConf chimneyFixerConf,
    ClosedPlaneMergerConf closedPlaneMergerConf,
    PlaneExtractionConf planeExtractionConf,
    RoofPointsCleanerConf roofPointsCleanerConf,
    PlaneDelimitationConf planeDelimitationConf,
    DelimitationFillerConf delimitationFillerConf) {

  @Builder(toBuilder = true)
  public record DelimitationFillerConf(int maxEmptyCell, int minCellPointsSize, double gridSize) {}

  @Builder(toBuilder = true)
  public record PlaneConf(double min2DArea, double compactness, int minPointsCount) {}

  @Builder(toBuilder = true)
  public record PlaneDelimitationConf(double concaveRatio, double simplificationEpsilon) {}

  @Builder(toBuilder = true)
  public record PlaneExtractionConf(int iteration, double pointContinuationThreshold) {}

  @Builder(toBuilder = true)
  public record ClosedPlaneMergerConf(
      double epsilonSlope, double epsilonZDistance, double epsilonXYDistance) {}

  @Builder(toBuilder = true)
  public record RoofPointsCleanerConf(double duplicateXYTolerance) {}

  @Builder(toBuilder = true)
  public record ChimneyFixerConf(double maxChimneyArea) {}

  public static Plane3DExtractorConf getDefault() {
    return Plane3DExtractorConf.builder()
        .roofPointsCleanerConf(RoofPointsCleanerConf.builder().duplicateXYTolerance(0.3).build())
        .chimneyFixerConf(ChimneyFixerConf.builder().maxChimneyArea(2).build())
        .planeConf(PlaneConf.builder().min2DArea(0.25).compactness(0.1).minPointsCount(10).build())
        .boxConf(BoxConf.builder().height(0.15).expansionSize(0.25).maxRefitPoints(100).build())
        .delimitationFillerConf(
            DelimitationFillerConf.builder()
                .gridSize(1)
                .maxEmptyCell(2)
                .minCellPointsSize(3)
                .build())
        .kernelConf(
            KernelConf.builder()
                .attempts(20)
                .maxLength(5)
                .degEpsilon(4)
                .minVectorNorm(1e-6)
                .squaredThreshold(0.75 * 0.75)
                .build())
        .planeDelimitationConf(
            PlaneDelimitationConf.builder().concaveRatio(0.2).simplificationEpsilon(0.55).build())
        .closedPlaneMergerConf(
            ClosedPlaneMergerConf.builder()
                .epsilonSlope(10)
                .epsilonZDistance(0.3)
                .epsilonXYDistance(3)
                .build())
        .planeExtractionConf(
            PlaneExtractionConf.builder().iteration(100).pointContinuationThreshold(0.5).build())
        .build();
  }
}
