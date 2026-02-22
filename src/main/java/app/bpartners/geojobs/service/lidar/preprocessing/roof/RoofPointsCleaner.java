package app.bpartners.geojobs.service.lidar.preprocessing.roof;

import app.bpartners.geojobs.model.lidar.LasPointGeometry;
import app.bpartners.geojobs.service.lidar.preprocessing.DuplicateXYPointsCleaner;
import java.util.*;

public record RoofPointsCleaner(DuplicateXYPointsCleaner duplicateXYPointsCleaner) {
  public RoofPointsCleaner(double xyToleranceMeters) {
    this(
        new DuplicateXYPointsCleaner(
            xyToleranceMeters, DuplicateXYPointsCleaner.DuplicateXYPointToKeep.HIGHEST));
  }

  public Set<LasPointGeometry> apply(Collection<LasPointGeometry> roofPoints) {
    return duplicateXYPointsCleaner.compute(roofPoints);
  }
}
