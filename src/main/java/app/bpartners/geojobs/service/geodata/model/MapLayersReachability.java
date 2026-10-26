package app.bpartners.geojobs.service.geodata.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record MapLayersReachability(
    String wmsBaseUrl, List<MapLayerReachability> layers, GeodataMapLayer actualLayer) {}
