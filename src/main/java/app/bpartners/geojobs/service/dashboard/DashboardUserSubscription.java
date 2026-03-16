package app.bpartners.geojobs.service.dashboard;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.time.Instant;

@JsonIgnoreProperties(ignoreUnknown = true)
public record DashboardUserSubscription(DashboardUserStatus status, Instant start, Instant end) {}
