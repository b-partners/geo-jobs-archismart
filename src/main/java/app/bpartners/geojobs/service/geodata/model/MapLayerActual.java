package app.bpartners.geojobs.service.geodata.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record MapLayerActual(String wmsBaseUrl, GeodataMapLayer layer) {}
