package app.bpartners.geojobs.service.dashboard.component;

import java.time.Instant;

public record Register2d(
    String detectionIdentifier,
    String zone,
    String address,
    Instant creationDatetime,
    DetectionInitiator initiator,
    String externalReference) {
  public Register2d(
      String detectionIdentifier,
      String zone,
      String address,
      Instant creationDatetime,
      DetectionInitiator initiator) {
    this(detectionIdentifier, zone, address, creationDatetime, initiator, null);
  }
}
