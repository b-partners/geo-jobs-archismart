package app.bpartners.geojobs.service;

import static app.bpartners.geojobs.file.FileWriter.createTempDirectory;
import static app.bpartners.geojobs.service.event.GeoJsonConversionTaskConsumer.GEO_JSON_EXTENSION;
import static app.bpartners.geojobs.service.event.GeoJsonConversionTaskConsumer.NEIGHBOUR_SIZE;

import app.bpartners.geojobs.endpoint.rest.postprocessing.BoundaryMerger;
import app.bpartners.geojobs.file.FileWriter;
import app.bpartners.geojobs.file.bucket.BucketComponent;
import app.bpartners.geojobs.model.geometry.VGG;
import app.bpartners.geojobs.model.geometry.VGGFactory;
import app.bpartners.geojobs.repository.model.detection.Detection;
import app.bpartners.geojobs.service.geojson.GeometryConverter;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.*;
import java.util.function.BiFunction;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class DetectionVGGUpdate implements BiFunction<VGG, Detection, Detection> {
  private static final String VGG_BUCKET_FOLDER = "vgg/";
  private final FileWriter fileWriter;
  private final BucketComponent bucketComponent;
  private final GeometryConverter geometryConverter;
  private final VGGFactory vggFactory;
  private final BoundaryMerger merger;

  public DetectionVGGUpdate(
      FileWriter fileWriter,
      BucketComponent bucketComponent,
      GeometryConverter geometryConverter,
      VGGFactory vggFactory) {
    this.fileWriter = fileWriter;
    this.bucketComponent = bucketComponent;
    this.geometryConverter = geometryConverter;
    this.vggFactory = vggFactory;
    merger = new BoundaryMerger(0, NEIGHBOUR_SIZE, false);
  }

  @Override
  public Detection apply(VGG vgg, Detection detection) {
    var vggAsByte = vgg.getBytes();
    return apply(detection, vggAsByte);
  }

  private Detection apply(Detection detection, byte[] vggAsByte) {
    var zoneName = detection.getZoneName();
    var zoneDetectionJobId = detection.getZdjId();
    var fileKey = VGG_BUCKET_FOLDER + zoneDetectionJobId + "/" + zoneName + ".json";
    var vggAsFile =
        fileWriter.write(vggAsByte, createTempDirectory(), zoneName + GEO_JSON_EXTENSION);
    bucketComponent.upload(vggAsFile, fileKey);
    return detection.toBuilder().vggFileKey(fileKey).build();
  }

  private Detection apply(Detection detection, byte[] vggAsByte, int featureNb) {
    var zoneName = detection.getZoneName();
    var zoneDetectionJobId = detection.getZdjId();
    var fileKey =
        VGG_BUCKET_FOLDER + zoneDetectionJobId + "/" + featureNb + "/" + zoneName + ".json";
    var vggAsFile =
        fileWriter.write(vggAsByte, createTempDirectory(), zoneName + GEO_JSON_EXTENSION);

    bucketComponent.upload(vggAsFile, fileKey);

    return featureNb == 0 ? detection.toBuilder().vggFileKey(fileKey).build() : detection;
  }

  public Detection apply(Collection<VGG> vggSet, Detection detection, int featureNb) {
    return apply(detection, getVggCollection(vggSet), featureNb);
  }

  public Detection apply(Collection<VGG> vggSet, Detection detection) {
    return apply(detection, getVggCollection(vggSet));
  }

  private byte[] getVggCollection(Collection<VGG> vggSet) {
    ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();
    try {
      return mapper.writeValueAsBytes(vggSet);
    } catch (JsonProcessingException e) {
      throw new RuntimeException(e);
    }
  }
}
