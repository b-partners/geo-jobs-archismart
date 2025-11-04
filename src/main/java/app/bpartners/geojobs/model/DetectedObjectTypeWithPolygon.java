package app.bpartners.geojobs.model;

import app.bpartners.geojobs.endpoint.rest.model.Point;
import app.bpartners.geojobs.repository.model.detection.DetectableType;
import java.util.List;

public record DetectedObjectTypeWithPolygon(DetectableType objectType, List<Point> pointList) {}
;
