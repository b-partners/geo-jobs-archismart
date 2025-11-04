package app.bpartners.geojobs.model.geometry.polygon;

import org.locationtech.jts.geom.Polygon;

public record PolygonAddress(String address, Polygon polygon) {}
