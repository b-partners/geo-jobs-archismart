package app.bpartners.geojobs.service.dashboard;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record DashboardUser(
    String id, String firstName, String lastName, DashboardUserSubscription subscription) {}
