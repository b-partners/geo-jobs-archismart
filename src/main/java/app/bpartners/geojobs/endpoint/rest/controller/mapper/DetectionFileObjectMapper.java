package app.bpartners.geojobs.endpoint.rest.controller.mapper;

import app.bpartners.geojobs.endpoint.rest.model.DetectionFileObject;
import app.bpartners.geojobs.endpoint.rest.model.DetectionFileType;
import app.bpartners.geojobs.file.bucket.BucketComponent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DetectionFileObjectMapper {
  private final BucketComponent bucketComponent;

  public DetectionFileObject toRest(
      app.bpartners.geojobs.repository.model.detection.DetectionFileObject detectionFileObject) {
    return new DetectionFileObject()
        .fileName(detectionFileObject.getFileName())
        .fileUrl(bucketComponent.presign(detectionFileObject.getBucketKey()))
        .fileType(
            detectionFileObject.getFileType() == null
                ? null
                : DetectionFileType.fromValue(detectionFileObject.getFileType().name()));
  }
}
