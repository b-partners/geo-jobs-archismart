package app.bpartners.geojobs.endpoint.rest.controller;

import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import app.bpartners.geojobs.endpoint.rest.model.CreateDetectionExportRequest;
import app.bpartners.geojobs.endpoint.rest.model.DetectionExportAttribute;
import app.bpartners.geojobs.endpoint.rest.model.DetectionExportRequest;
import app.bpartners.geojobs.endpoint.rest.validator.CreateDetectionExportRequestValidator;
import app.bpartners.geojobs.service.DetectionExportService;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

class DetectionControllerTest {
  DetectionExportService detectionExportServiceMock = mock();
  CreateDetectionExportRequestValidator createDetectionExportRequestValidatorMock = mock();
  DetectionController subject =
      new DetectionController(
          detectionExportServiceMock, createDetectionExportRequestValidatorMock);

  @Test
  void export_detections_ok() {
    var communityId = "community_id";
    var exportId = randomUUID().toString();
    var from = LocalDate.of(2024, 1, 1);
    var to = LocalDate.of(2024, 1, 31);
    var additionalAttributes =
        List.of(DetectionExportAttribute.AREA, DetectionExportAttribute.ZONE_NAME);
    var presignUrl = "https://bucket/presigned-url";
    var createDetectionExportRequest =
        new CreateDetectionExportRequest()
            .from(from)
            .to(to)
            .additionalExportedAttributes(additionalAttributes);
    when(detectionExportServiceMock.requestDetectionExport(
            communityId, exportId, from, to, additionalAttributes))
        .thenReturn(presignUrl);

    var actual = subject.exportDetections(communityId, exportId, createDetectionExportRequest);

    var expected =
        new DetectionExportRequest()
            .id(exportId)
            .from(from)
            .to(to)
            .additionalExportedAttributes(additionalAttributes)
            .fileUrl(presignUrl);
    assertEquals(expected, actual);
    verify(detectionExportServiceMock)
        .requestDetectionExport(communityId, exportId, from, to, additionalAttributes);
  }
}
