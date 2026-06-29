package app.bpartners.geojobs.service;

import static app.bpartners.geojobs.file.FileWriter.createTempDirectory;
import static app.bpartners.geojobs.repository.model.detection.DetectionFileType.ASSEMBLE_VGG;
import static app.bpartners.geojobs.service.event.GeoJsonConversionTaskConsumer.GEO_JSON_EXTENSION;
import static java.time.Instant.now;
import static java.util.UUID.randomUUID;

import app.bpartners.geojobs.file.FileWriter;
import app.bpartners.geojobs.file.bucket.BucketComponent;
import app.bpartners.geojobs.model.geometry.VGG;
import app.bpartners.geojobs.model.geometry.VGGFactory;
import app.bpartners.geojobs.repository.DetectionFileObjectRepository;
import app.bpartners.geojobs.repository.model.detection.Detection;
import app.bpartners.geojobs.repository.model.detection.DetectionFileObject;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class DetectionVGGUpdate {
  private static final String VGG_BUCKET_FOLDER = "vgg/";
  private final FileWriter fileWriter;
  private final BucketComponent bucketComponent;
  private final VGGFactory vggFactory;
  private final DetectionFileObjectRepository detectionFileObjectRepository;

  public DetectionVGGUpdate(
      FileWriter fileWriter,
      BucketComponent bucketComponent,
      VGGFactory vggFactory,
      DetectionFileObjectRepository detectionFileObjectRepository) {
    this.fileWriter = fileWriter;
    this.bucketComponent = bucketComponent;
    this.vggFactory = vggFactory;
    this.detectionFileObjectRepository = detectionFileObjectRepository;
  }

  private Detection apply(Detection detection, byte[] vggAsByte, String uniqueProperty) {
    var zoneName = detection.getZoneName();
    var zoneDetectionJobId = detection.getZdjId();
    var fileKey =
        VGG_BUCKET_FOLDER + zoneDetectionJobId + "/" + uniqueProperty + "/" + zoneName + ".json";
    var vggAsFile =
        fileWriter.write(vggAsByte, createTempDirectory(), zoneName + GEO_JSON_EXTENSION);

    if (detection.isDebugMode()) {
      detectionFileObjectRepository.save(
          DetectionFileObject.builder()
              .id(randomUUID().toString())
              .detectionIdentifier(detection.getId())
              .fileName(replaceSeparator(fileKey))
              .fileType(ASSEMBLE_VGG)
              .bucketKey(fileKey)
              .creationDatetime(now())
              .build());
    }

    bucketComponent.upload(vggAsFile, fileKey);

    return detection.getProvidedGeoJsonZone() != null
            && detection.getProvidedGeoJsonZone().size() == 1
        ? detection.toBuilder().vggFileKey(fileKey).build()
        : detection;
  }

  public Detection apply(Collection<VGG> vggSet, Detection detection, String uniqueProperty) {
    var unifiedVggSet = vggFactory.unifyVggSet(vggSet);
    return apply(detection, getVggCollection(unifiedVggSet), uniqueProperty);
  }

  private byte[] getVggCollection(Collection<VGG> vggSet) {
    ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();
    try {
      return mapper.writeValueAsBytes(vggSet);
    } catch (JsonProcessingException e) {
      throw new RuntimeException(e);
    }
  }

  private static String replaceSeparator(String path) {
    if (path == null) {
      return null;
    }
    return path.replace('/', '_');
  }
}
