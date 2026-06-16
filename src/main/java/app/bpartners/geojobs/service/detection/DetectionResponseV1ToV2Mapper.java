package app.bpartners.geojobs.service.detection;

import static java.util.UUID.randomUUID;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import org.springframework.stereotype.Component;

@Component
public class DetectionResponseV1ToV2Mapper
    implements Function<DetectionResponse, DetectionResponseV2> {

  @Override
  public DetectionResponseV2 apply(DetectionResponse v1) {
    if (v1 == null) {
      return null;
    }

    Map<String, DetectionResponseV2.ImageData> images = new HashMap<>();
    if (v1.getRstRaw() != null) {
      v1.getRstRaw().forEach((key, imageData) -> images.put(key, toV2ImageData(imageData)));
    }
    if (v1.getVegetationData() != null) {
      images.put("vegetation_data_" + randomUUID(), toV2ImageData(v1.getVegetationData()));
    }

    return DetectionResponseV2.builder().images(images).build();
  }

  private DetectionResponseV2.ImageData toV2ImageData(DetectionResponse.ImageData v1) {
    if (v1 == null) {
      return null;
    }
    Map<String, DetectionResponseV2.ImageData.Region> regions = new HashMap<>();
    if (v1.getRegions() != null) {
      v1.getRegions().forEach((key, region) -> regions.put(key, toV2Region(region)));
    }
    return DetectionResponseV2.ImageData.builder()
        .fileref(v1.getFileref())
        .size(String.valueOf(v1.getSize()))
        .filename(v1.getFilename())
        .base64ImgData(v1.getBase64ImgData())
        .fileAttributes(
            v1.getFileAttributes() != null
                ? new HashMap<>(v1.getFileAttributes())
                : new HashMap<>())
        .regions(regions)
        .build();
  }

  private DetectionResponseV2.ImageData.Region toV2Region(DetectionResponse.ImageData.Region v1) {
    if (v1 == null) {
      return null;
    }
    return DetectionResponseV2.ImageData.Region.builder()
        .regionAttributes(
            v1.getRegionAttributes() != null
                ? new HashMap<>(v1.getRegionAttributes())
                : new HashMap<>())
        .shapeAttributes(toV2ShapeAttributes(v1.getShapeAttributes()))
        .build();
  }

  private DetectionResponseV2.ImageData.ShapeAttributes toV2ShapeAttributes(
      DetectionResponse.ImageData.ShapeAttributes v1) {
    if (v1 == null) {
      return null;
    }
    return DetectionResponseV2.ImageData.ShapeAttributes.builder()
        .name(v1.getName())
        .allPointsX(v1.getAllPointsX())
        .allPointsY(v1.getAllPointsY())
        .build();
  }
}
