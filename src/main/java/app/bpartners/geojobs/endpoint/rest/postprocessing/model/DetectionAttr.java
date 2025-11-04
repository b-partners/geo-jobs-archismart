package app.bpartners.geojobs.endpoint.rest.postprocessing.model;

import app.bpartners.geojobs.endpoint.rest.model.DetectableObjectType;
import java.math.BigDecimal;

public record DetectionAttr(
    DetectableObjectType objectType, String detectorVersion, BigDecimal confidence) {}
