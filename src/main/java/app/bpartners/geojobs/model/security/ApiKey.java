package app.bpartners.geojobs.model.security;

import java.time.Instant;

public record ApiKey(String apiKey, Instant creationDatetime) {}
