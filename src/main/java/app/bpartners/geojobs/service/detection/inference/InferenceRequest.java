package app.bpartners.geojobs.service.detection.inference;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;

@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public record InferenceRequest(
    String modelCardId,
    String image1,
    String mask1,
    String image2,
    String mask2,
    String filename,
    Integer zoomLevel,
    Double spatialResolution) {}
