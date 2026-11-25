package app.bpartners.geojobs.service.detection.inference;

import app.bpartners.geojobs.repository.model.detection.DetectableType;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ModelCardMapping(
    DetectableType objectType,
    String modelCardId,
    Integer zoomLevel,
    Double spatialResolution,
    Double inferenceSpatialResolution) {

  public ModelCardMapping(
      DetectableType objectType, String modelCardId, Integer zoomLevel, Double spatialResolution) {
    this(objectType, modelCardId, zoomLevel, spatialResolution, null);
  }

  public double spatialResolutionSentToInference() {
    return inferenceSpatialResolution == null ? spatialResolution : inferenceSpatialResolution;
  }

  public boolean isInferenceSpatialResolutionForced() {
    return inferenceSpatialResolution != null
        && !inferenceSpatialResolution.equals(spatialResolution);
  }
}
