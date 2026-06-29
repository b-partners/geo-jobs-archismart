package app.bpartners.geojobs.service.event;

import static app.bpartners.geojobs.repository.model.detection.DetectionFileType.*;
import static java.io.File.createTempFile;
import static java.time.Instant.now;
import static java.util.UUID.randomUUID;

import app.bpartners.geojobs.endpoint.event.model.FeatureAnnotatedImageRequested;
import app.bpartners.geojobs.file.bucket.BucketComponent;
import app.bpartners.geojobs.repository.DetectionFileObjectRepository;
import app.bpartners.geojobs.repository.DetectionRepository;
import app.bpartners.geojobs.repository.model.detection.DetectionFileObject;
import app.bpartners.geojobs.service.VggImageAnnotator;
import java.util.function.Consumer;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class FeatureAnnotatedImageRequestedService
    implements Consumer<FeatureAnnotatedImageRequested> {
  private final DetectionRepository detectionRepository;
  private final BucketComponent bucketComponent;
  private final VggImageAnnotator vggImageAnnotator;
  private final DetectionFileObjectRepository detectionFileObjectRepository;

  @SneakyThrows
  @Override
  public void accept(FeatureAnnotatedImageRequested event) {
    var detectionIdentifier = event.getDetectionIdentifier();
    var detection = detectionRepository.findById(detectionIdentifier).orElse(null);
    if (detection == null) {
      log.error("Unable to find detection.id {}", detectionIdentifier);
      return;
    }
    var assembleImagesFileObjects =
        detection.getFileObjects().stream()
            .filter(detectionFileObject -> ASSEMBLE_IMAGE.equals(detectionFileObject.getFileType()))
            .toList();

    var assembleVggFileObjects =
        detection.getFileObjects().stream()
            .filter(detectionFileObject -> ASSEMBLE_VGG.equals(detectionFileObject.getFileType()))
            .toList();

    if (assembleImagesFileObjects.size() != 1 || assembleVggFileObjects.size() != 1) {
      log.error(
          "Detection.id {} owns multiple assemble image and vgg not supported for now",
          detectionIdentifier);
      return;
    }
    var assembleImageFile =
        bucketComponent.download(assembleImagesFileObjects.getFirst().getBucketKey());
    var assembleVggFile =
        bucketComponent.download(assembleVggFileObjects.getFirst().getBucketKey());
    var annotatedImageFile =
        vggImageAnnotator.annotate(
            assembleVggFile, assembleImageFile, createTempFile(randomUUID().toString(), ".png"));

    var annotatedImageFileKey =
        "annotated_images/" + detection.getId() + "/" + randomUUID() + ".png";

    bucketComponent.upload(annotatedImageFile, annotatedImageFileKey);

    if (detection.isDebugMode()) {
      detectionFileObjectRepository.save(
          DetectionFileObject.builder()
              .id(randomUUID().toString())
              .detectionIdentifier(detection.getId())
              .fileType(ANNOTATED_ASSEMBLE_IMAGE)
              .bucketKey(annotatedImageFileKey)
              .fileName(replaceSeparator(annotatedImageFileKey))
              .creationDatetime(now())
              .build());
    }
  }

  private static String replaceSeparator(String path) {
    if (path == null) {
      return null;
    }
    return path.replace('/', '_');
  }
}
