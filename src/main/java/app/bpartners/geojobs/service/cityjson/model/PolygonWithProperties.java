package app.bpartners.geojobs.service.cityjson.model;

import java.io.Serializable;
import java.util.Map;
import lombok.Builder;
import org.locationtech.jts.geom.Polygon;

@Builder
public record PolygonWithProperties(Polygon polygon, Map<String, Object> properties)
    implements Serializable {}
