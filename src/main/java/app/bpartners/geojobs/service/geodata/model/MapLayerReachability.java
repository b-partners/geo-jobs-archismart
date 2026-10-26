package app.bpartners.geojobs.service.geodata.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record MapLayerReachability(GeodataMapLayer layer, boolean reachable) {}
