package app.bpartners.geojobs.model.geometry.route;

import org.locationtech.jts.geom.Polygon;

public record Route(Polygon polygon, ObjectType type) {}
