package app.bpartners.geojobs.service;

import static java.time.ZoneOffset.UTC;
import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.data.domain.Pageable.unpaged;

import app.bpartners.geojobs.endpoint.rest.model.DetectionExportAttribute;
import app.bpartners.geojobs.file.bucket.BucketComponent;
import app.bpartners.geojobs.repository.CommunityAuthorizationRepository;
import app.bpartners.geojobs.repository.DetectionRepository;
import app.bpartners.geojobs.repository.model.community.CommunityAuthorization;
import app.bpartners.geojobs.repository.model.detection.Detection;
import java.io.File;
import java.time.LocalDate;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class DetectionExportServiceTest {
  CommunityAuthorizationRepository communityAuthorizationRepositoryMock = mock();
  DetectionRepository detectionRepositoryMock = mock();
  DetectionConverter detectionConverterMock = mock();
  BucketComponent bucketComponentMock = mock();

  DetectionExportService subject =
      new DetectionExportService(
          communityAuthorizationRepositoryMock,
          detectionRepositoryMock,
          detectionConverterMock,
          bucketComponentMock);

  @Test
  void request_detection_export_ok() {
    var communityOwnerId = "community_owner_id";
    var communityInternalId = "community_internal_id";
    var exportId = randomUUID().toString();
    var from = LocalDate.of(2024, 1, 1);
    var to = LocalDate.of(2024, 1, 31);
    var additionalAttributes =
        List.of(DetectionExportAttribute.AREA, DetectionExportAttribute.ZONE_NAME);
    var community = CommunityAuthorization.builder().id(communityInternalId).build();
    var detections = List.of(new Detection());
    var exportedFile = new File("exported_detections.xlsx");
    var presignUrl = "https://bucket/presigned-url";
    when(communityAuthorizationRepositoryMock.findById(communityOwnerId))
        .thenReturn(Optional.of(community));
    when(detectionRepositoryMock.findByCommunityOwnerIdAndCriteria(
            eq(communityInternalId),
            eq(from.atStartOfDay().toInstant(UTC)),
            eq(to.atStartOfDay().plusDays(1L).minusSeconds(1L).toInstant(UTC)),
            eq(unpaged())))
        .thenReturn(detections);
    when(detectionConverterMock.apply(detections, additionalAttributes)).thenReturn(exportedFile);
    when(bucketComponentMock.presign(exportId)).thenReturn(presignUrl);

    var actual =
        subject.requestDetectionExport(communityOwnerId, exportId, from, to, additionalAttributes);

    assertEquals(presignUrl, actual);
    verify(bucketComponentMock).upload(exportedFile, exportId);
  }

  @Test
  void request_detection_export_throws_when_community_not_found() {
    var communityOwnerId = "unknown_community_owner_id";
    var exportId = randomUUID().toString();
    var from = LocalDate.of(2024, 1, 1);
    var to = LocalDate.of(2024, 1, 31);
    var additionalAttributes = List.of(DetectionExportAttribute.AREA);
    when(communityAuthorizationRepositoryMock.findById(communityOwnerId))
        .thenReturn(Optional.empty());

    var actual =
        assertThrows(
            NoSuchElementException.class,
            () ->
                subject.requestDetectionExport(
                    communityOwnerId, exportId, from, to, additionalAttributes));

    assertEquals("No value present", actual.getMessage());
    verify(bucketComponentMock, org.mockito.Mockito.never()).upload(any(), any());
  }
}
