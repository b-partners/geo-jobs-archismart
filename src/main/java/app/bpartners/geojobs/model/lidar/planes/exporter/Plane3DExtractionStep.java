package app.bpartners.geojobs.model.lidar.planes.exporter;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum Plane3DExtractionStep {
  INIT_POINTS(0),
  RAW_PLANE_EXTRACTION(1),
  CLOSED_PLANE_MERGING(2),
  RAW_DELIMITATION_EXTRACTION(3),
  DELIMITATION_SIMPLIFICATION(4),
  ITERATION_POINTS_EVOLUTION(5),
  CHIMNEY_POLYGON(6),
  CHIMNEY_CONVEXE_POLYGON(7),
  CHIMNEY_FIXED_POLYGON(8),
  RAW_PLANE_KERNEL(9),
  BEFORE_REMOVING_SKINNY_ARM(10),
  AFTER_REMOVING_SKINNY_ARM(10),
  SKINNY_ARM_TO_KEEP_CELL_POLYGON(11),
  SKINNY_ARM_TO_DELETE_CELL_POLYGON(12),
  SKINNY_ARM_INVALID_CELL_POLYGON(13),
  SKINNY_ARM_VALID_CELL_POLYGON(14),
  DEBUG(15);

  private final int stepIndex;

  public String toFilePrefix() {
    return String.format("Step%d_%s", stepIndex, this);
  }
}
