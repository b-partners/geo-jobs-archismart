package app.bpartners.geojobs.service.geodata.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record GeodataMapLayer(
    String id,
    String name,
    Integer year,
    String source,
    String departementName,
    String maximumZoomLevel,
    Integer precisionLevelInCm) {}
