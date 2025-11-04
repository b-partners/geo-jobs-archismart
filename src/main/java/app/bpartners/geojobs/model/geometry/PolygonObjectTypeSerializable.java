package app.bpartners.geojobs.model.geometry;

import app.bpartners.geojobs.repository.model.detection.DetectableType;
import java.io.Serializable;

public record PolygonObjectTypeSerializable(String polygonAsString, DetectableType detectableType)
    implements Serializable {}
