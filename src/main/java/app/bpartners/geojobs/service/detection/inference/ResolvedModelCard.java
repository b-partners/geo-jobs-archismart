package app.bpartners.geojobs.service.detection.inference;

public record ResolvedModelCard(
    DeployedModelCard modelCard, int zoomLevel, double spatialResolution) {
  public String modelCardId() {
    return modelCard.modelCardId();
  }
}
