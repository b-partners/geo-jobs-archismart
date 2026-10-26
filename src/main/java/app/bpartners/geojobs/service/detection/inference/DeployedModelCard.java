package app.bpartners.geojobs.service.detection.inference;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record DeployedModelCard(
    String modelCardId,
    ProblemType problemType,
    int imagesRequired,
    boolean maskRequired,
    Integer imageWidth,
    Integer imageHeight,
    Boolean runnable) {

  public enum ProblemType {
    SEMANTIC_SEGMENTATION,
    CHANGE_DETECTION,
    INSTANCE_SEGMENTATION,
    OBJECT_DETECTION,
    CLASSIFICATION
  }

  public boolean isRunnable() {
    return !Boolean.FALSE.equals(runnable);
  }
}
