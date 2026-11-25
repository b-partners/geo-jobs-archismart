package app.bpartners.geojobs.service;

public record DetectionReuseFingerprint(
    String zoneHash, String inferenceSignature, ZoneBounds zoneBounds) {
  public DetectionReuseFingerprint(String zoneHash, String inferenceSignature) {
    this(zoneHash, inferenceSignature, null);
  }
}
