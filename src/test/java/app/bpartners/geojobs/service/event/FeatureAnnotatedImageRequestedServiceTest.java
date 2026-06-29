package app.bpartners.geojobs.service.event;

import static app.bpartners.geojobs.repository.model.detection.DetectionFileType.ANNOTATED_ASSEMBLE_IMAGE;
import static app.bpartners.geojobs.repository.model.detection.DetectionFileType.ASSEMBLE_IMAGE;
import static app.bpartners.geojobs.repository.model.detection.DetectionFileType.ASSEMBLE_VGG;
import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import app.bpartners.geojobs.endpoint.event.model.FeatureAnnotatedImageRequested;
import app.bpartners.geojobs.file.bucket.BucketComponent;
import app.bpartners.geojobs.repository.DetectionFileObjectRepository;
import app.bpartners.geojobs.repository.DetectionRepository;
import app.bpartners.geojobs.repository.model.detection.Detection;
import app.bpartners.geojobs.repository.model.detection.DetectionFileObject;
import app.bpartners.geojobs.repository.model.detection.DetectionFileType;
import app.bpartners.geojobs.service.VggImageAnnotator;
import java.io.File;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class FeatureAnnotatedImageRequestedServiceTest {
  DetectionRepository detectionRepository = mock();
  BucketComponent bucketComponent = mock();
  VggImageAnnotator vggImageAnnotator = mock();
  DetectionFileObjectRepository detectionFileObjectRepository = mock();

  FeatureAnnotatedImageRequestedService subject =
      new FeatureAnnotatedImageRequestedService(
          detectionRepository, bucketComponent, vggImageAnnotator, detectionFileObjectRepository);

  private static final String DETECTION_ID = randomUUID().toString();
  private static final String IMAGE_KEY = randomUUID().toString();
  private static final String VGG_KEY = randomUUID().toString();

  @Test
  void upload_annotated_image_and_save_detection_file_object() throws Exception {
    var detection =
        Detection.builder()
            .id(DETECTION_ID)
            .debugMode(true)
            .fileObjects(
                List.of(fileObject(ASSEMBLE_IMAGE, IMAGE_KEY), fileObject(ASSEMBLE_VGG, VGG_KEY)))
            .build();
    var imageFile = mock(File.class);
    var vggFile = mock(File.class);
    var annotatedFile = mock(File.class);
    when(detectionRepository.findById(DETECTION_ID)).thenReturn(Optional.of(detection));
    when(bucketComponent.download(IMAGE_KEY)).thenReturn(imageFile);
    when(bucketComponent.download(VGG_KEY)).thenReturn(vggFile);
    when(vggImageAnnotator.annotate(eq(vggFile), eq(imageFile), any(File.class)))
        .thenReturn(annotatedFile);

    subject.accept(new FeatureAnnotatedImageRequested(DETECTION_ID));

    var uploadKeyCaptor = ArgumentCaptor.forClass(String.class);
    verify(bucketComponent).upload(eq(annotatedFile), uploadKeyCaptor.capture());
    var uploadKey = uploadKeyCaptor.getValue();
    assertTrue(
        uploadKey.startsWith("annotated_images/" + DETECTION_ID + "/"),
        "unexpected upload key " + uploadKey);
    assertTrue(uploadKey.endsWith(".png"), "unexpected upload key " + uploadKey);

    var savedCaptor = ArgumentCaptor.forClass(DetectionFileObject.class);
    verify(detectionFileObjectRepository).save(savedCaptor.capture());
    var saved = savedCaptor.getValue();
    assertEquals(ANNOTATED_ASSEMBLE_IMAGE, saved.getFileType());
    assertEquals(DETECTION_ID, saved.getDetectionIdentifier());
    assertEquals(uploadKey, saved.getBucketKey());
  }

  @Test
  void detection_not_found_ko() {
    when(detectionRepository.findById(DETECTION_ID)).thenReturn(Optional.empty());

    subject.accept(new FeatureAnnotatedImageRequested(DETECTION_ID));

    verifyNoInteractions(bucketComponent, vggImageAnnotator, detectionFileObjectRepository);
  }

  private static DetectionFileObject fileObject(DetectionFileType fileType, String bucketKey) {
    return DetectionFileObject.builder().fileType(fileType).bucketKey(bucketKey).build();
  }
}
