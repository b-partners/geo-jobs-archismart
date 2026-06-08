package app.bpartners.geojobs.service.osm.model;

import java.util.Map;
import org.locationtech.jts.geom.Polygon;

public record BuildingMatch(
    Long osmId, Polygon geometry, Map<String, String> tags, Double distanceMeters) {}
