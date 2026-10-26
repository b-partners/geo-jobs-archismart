package app.bpartners.geojobs.service.dashboard.component;

import java.time.Instant;

public record CreateDetectionTracking(
    String zone,
    String address,
    Instant creationDatetime,
    DetectionInitiator initiator,
    String detectionIdentifier) {}
