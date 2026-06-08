package app.bpartners.geojobs.model.geometry;

import app.bpartners.geojobs.repository.model.detection.DetectableType;
import org.locationtech.jts.geom.MultiPolygon;

public record MultiPolygonObjectType(MultiPolygon multiPolygon, DetectableType objectType) {}
