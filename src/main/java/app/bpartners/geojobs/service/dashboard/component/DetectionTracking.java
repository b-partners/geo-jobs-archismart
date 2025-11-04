package app.bpartners.geojobs.service.dashboard.component;

import java.time.Instant;

public record DetectionTracking(
    String id,
    String zone,
    String address,
    Instant creationDatetime,
    DetectionInitiator initiator) {}
