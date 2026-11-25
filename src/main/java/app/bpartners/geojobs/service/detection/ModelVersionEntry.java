package app.bpartners.geojobs.service.detection;

import app.bpartners.geojobs.repository.model.detection.DetectableType;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ModelVersionEntry(
    DetectableType objectType,
    String modelCardId,
    Integer zoomLevel,
    Double spatialResolution,
    Double inferenceSpatialResolution,
    String url,
    DetectionApiVersion version) {}
