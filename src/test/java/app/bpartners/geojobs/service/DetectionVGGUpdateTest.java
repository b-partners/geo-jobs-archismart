package app.bpartners.geojobs.service;

import static app.bpartners.geojobs.repository.model.detection.DetectionFileType.ASSEMBLE_VGG;
import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import app.bpartners.geojobs.file.FileWriter;
import app.bpartners.geojobs.file.bucket.BucketComponent;
import app.bpartners.geojobs.file.hash.FileHash;
import app.bpartners.geojobs.model.geometry.VGG;
import app.bpartners.geojobs.model.geometry.VGGFactory;
import app.bpartners.geojobs.repository.DetectionFileObjectRepository;
import app.bpartners.geojobs.repository.model.detection.Detection;
import app.bpartners.geojobs.repository.model.detection.DetectionFileObject;
import java.io.File;
import java.util.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class DetectionVGGUpdateTest {

  FileWriter fileWriterMock = mock();
  BucketComponent bucketComponentMock = mock();
  VGGFactory vggFactoryMock = mock();
  DetectionFileObjectRepository detectionFileObjectRepositoryMock =
      mock(DetectionFileObjectRepository.class);

  @AfterEach
  void tearDown() {
    reset(vggFactoryMock);
    reset(bucketComponentMock);
    reset(fileWriterMock);
  }

  @Test
  void apply_with_vgg_set_and_feature_number() {
    VGG vgg = mock();
    Set<VGG> vggSet = Set.of(vgg);
    var detectionIdentifier = randomUUID().toString();
    Detection detection = mock();
    byte[] vggAsBytes = new byte[0];
    File vggAsFile = mock();
    var featureId = randomUUID().toString();

    when(vggFactoryMock.unifyVggSet(vggSet)).thenReturn(vggSet);
    when(detection.getId()).thenReturn(detectionIdentifier);
    when(detection.getZoneName()).thenReturn("dummy zoneName");
    when(detection.getZdjId()).thenReturn("dummy zdjId");
    when(detection.getProvidedGeoJsonZone()).thenReturn(List.of(mock(), mock()));
    when(detection.isDebugMode()).thenReturn(true);
    when(fileWriterMock.write(eq(vggAsBytes), any(), any())).thenReturn(vggAsFile);
    when(bucketComponentMock.upload(any(), any())).thenReturn(mock(FileHash.class));
    when(detectionFileObjectRepositoryMock.save(any()))
        .thenAnswer(invocationOnMock -> invocationOnMock.getArgument(0));

    var subject =
        new DetectionVGGUpdate(
            fileWriterMock, bucketComponentMock, vggFactoryMock, detectionFileObjectRepositoryMock);

    var actual = subject.apply(vggSet, detection, featureId);

    assertEquals(detection, actual);
    var detectionFileObjectCaptor = ArgumentCaptor.forClass(DetectionFileObject.class);
    verify(detectionFileObjectRepositoryMock, times(1)).save(detectionFileObjectCaptor.capture());
    var savedDetectionFileObject = detectionFileObjectCaptor.getValue();
    assertEquals(
        DetectionFileObject.builder()
            .id(savedDetectionFileObject.getId())
            .detectionIdentifier(detectionIdentifier)
            .fileName(savedDetectionFileObject.getFileName())
            .bucketKey("vgg/dummy zdjId/" + featureId + "/dummy zoneName.json")
            .fileType(ASSEMBLE_VGG)
            .creationDatetime(savedDetectionFileObject.getCreationDatetime())
            .build(),
        savedDetectionFileObject);
  }
}
