package app.bpartners.geojobs.endpoint.rest.controller;

import app.bpartners.geojobs.endpoint.rest.model.CreateDetectionExportRequest;
import app.bpartners.geojobs.endpoint.rest.model.DetectionExportRequest;
import app.bpartners.geojobs.endpoint.rest.validator.CreateDetectionExportRequestValidator;
import app.bpartners.geojobs.service.DetectionExportService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class DetectionController {
  private final DetectionExportService detectionExportService;
  private final CreateDetectionExportRequestValidator createDetectionExportRequestValidator;

  @PutMapping("/communities/{id}/detectionsExport/{exportId}")
  public DetectionExportRequest exportDetections(
      @PathVariable(name = "id") String communityIdentifier,
      @PathVariable(name = "exportId") String exportId,
      @RequestBody CreateDetectionExportRequest createDetectionExportRequest) {
    createDetectionExportRequestValidator.accept(createDetectionExportRequest);

    var presignUrl =
        detectionExportService.requestDetectionExport(
            communityIdentifier,
            exportId,
            createDetectionExportRequest.getFrom(),
            createDetectionExportRequest.getTo(),
            createDetectionExportRequest.getAdditionalExportedAttributes());

    return new DetectionExportRequest()
        .id(exportId)
        .from(createDetectionExportRequest.getFrom())
        .to(createDetectionExportRequest.getTo())
        .additionalExportedAttributes(
            createDetectionExportRequest.getAdditionalExportedAttributes())
        .fileUrl(presignUrl);
  }
}
