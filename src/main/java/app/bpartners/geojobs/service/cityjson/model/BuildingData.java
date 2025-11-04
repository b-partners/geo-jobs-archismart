package app.bpartners.geojobs.service.cityjson.model;

import java.util.List;
import java.util.Map;
import lombok.Builder;

@Builder
public record BuildingData(
    String id,
    List<PolygonWithProperties> roofs,
    List<PolygonWithProperties> walls,
    List<PolygonWithProperties> grounds,
    Map<String, Object> properties) {}
