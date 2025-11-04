package app.bpartners.geojobs.model.geometry;

import app.bpartners.geojobs.repository.model.detection.DetectableType;
import org.locationtech.jts.geom.Polygon;

public record PolygonObjectType(Polygon polygon, DetectableType objectType) {}
