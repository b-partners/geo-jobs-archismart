package app.bpartners.geojobs.service.dashboard.component;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record Account(String id, String name, Boolean active) {}
