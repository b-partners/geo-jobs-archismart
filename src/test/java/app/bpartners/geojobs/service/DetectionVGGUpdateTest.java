package app.bpartners.geojobs.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import app.bpartners.geojobs.file.FileWriter;
import app.bpartners.geojobs.file.bucket.BucketComponent;
import app.bpartners.geojobs.file.hash.FileHash;
import app.bpartners.geojobs.model.geometry.VGG;
import app.bpartners.geojobs.model.geometry.VGGFactory;
import app.bpartners.geojobs.repository.model.detection.Detection;
import java.io.File;
import java.util.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class DetectionVGGUpdateTest {

  FileWriter fileWriterMock = mock();
  BucketComponent bucketComponentMock = mock();
  VGGFactory vggFactoryMock = mock();

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
    Detection detection = mock();
    byte[] vggAsBytes = new byte[0];
    File vggAsFile = mock();
    int featureNb = 2;

    when(vggFactoryMock.unifyVggSet(vggSet)).thenReturn(vggSet);
    when(detection.getZoneName()).thenReturn("dummy zoneName");
    when(detection.getZdjId()).thenReturn("dummy zdjId");
    when(fileWriterMock.write(eq(vggAsBytes), any(), any())).thenReturn(vggAsFile);
    when(bucketComponentMock.upload(any(), any())).thenReturn(mock(FileHash.class));

    var subject = new DetectionVGGUpdate(fileWriterMock, bucketComponentMock, vggFactoryMock);

    var actual = subject.apply(vggSet, detection, featureNb);

    assertEquals(detection, actual);
  }
}
