package app.bpartners.geojobs.endpoint.objectmapper;

import static app.bpartners.geojobs.endpoint.rest.model.DetectionFileType.IMAGE;
import static app.bpartners.geojobs.repository.model.detection.DetectionFileType.TILE_IMAGE;
import static java.time.Instant.now;
import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import app.bpartners.geojobs.endpoint.rest.controller.mapper.DetectionFileObjectMapper;
import app.bpartners.geojobs.file.bucket.BucketComponent;
import app.bpartners.geojobs.repository.model.detection.DetectionFileObject;
import org.junit.jupiter.api.Test;

class DetectionFileObjectMapperTest {
  BucketComponent bucketComponentMock = mock(BucketComponent.class);
  DetectionFileObjectMapper subject = new DetectionFileObjectMapper(bucketComponentMock);

  @Test
  void apply_detection_file_object_to_rest() {
    var detectionIdentifier = randomUUID().toString();
    var detectionFileObjectIdentifier = randomUUID().toString();
    var bucketKey = "bucketKey";
    var fileName = "fileName";
    var presignedUrl = "presignedUrl";
    when(bucketComponentMock.presign(bucketKey)).thenReturn(presignedUrl);

    var actual =
        subject.toRest(
            DetectionFileObject.builder()
                .id(detectionFileObjectIdentifier)
                .detectionIdentifier(detectionIdentifier)
                .bucketKey(bucketKey)
                .fileName(fileName)
                .fileType(TILE_IMAGE)
                .creationDatetime(now())
                .build());

    assertEquals(
        new app.bpartners.geojobs.endpoint.rest.model.DetectionFileObject()
            .fileName(fileName)
            .fileType(IMAGE)
            .fileUrl(presignedUrl),
        actual);
  }
}
