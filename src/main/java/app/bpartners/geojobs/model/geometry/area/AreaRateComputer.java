package app.bpartners.geojobs.model.geometry.area;

import app.bpartners.geojobs.repository.model.detection.DetectableType;

public abstract class AreaRateComputer {
  abstract double compute(DetectableType detectableType);

  abstract double getGlobalRate();
}
