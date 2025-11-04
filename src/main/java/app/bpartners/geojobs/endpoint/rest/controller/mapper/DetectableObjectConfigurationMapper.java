package app.bpartners.geojobs.endpoint.rest.controller.mapper;

import static java.util.UUID.randomUUID;

import app.bpartners.geojobs.file.bucket.BucketConf;
import app.bpartners.geojobs.repository.model.detection.DetectableObjectConfiguration;
import java.math.BigDecimal;
import java.util.Objects;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class DetectableObjectConfigurationMapper {
  private final DetectableObjectTypeMapper typeMapper;
  private BucketConf bucketConf;
  private final Double DEFAULT_CONFIDENCE_FOR_IN_DOUBT_DETECTION = 1.0;

  public DetectableObjectConfiguration toDomain(
      String jobId, app.bpartners.geojobs.endpoint.rest.model.DetectableObjectConfiguration rest) {
    return DetectableObjectConfiguration.builder()
        .id(randomUUID().toString())
        .detectionJobId(jobId)
        .objectType(typeMapper.toDomain(Objects.requireNonNull(rest.getType())))
        .minConfidenceForDetection(
            rest.getConfidence() != null
                ? rest.getConfidence().doubleValue()
                : DEFAULT_CONFIDENCE_FOR_IN_DOUBT_DETECTION)
        .bucketStorageName(
            rest.getBucketStorageName() != null
                ? rest.getBucketStorageName()
                : bucketConf.getBucketName())
        .build();
  }

  public app.bpartners.geojobs.endpoint.rest.model.DetectableObjectConfiguration toRest(
      DetectableObjectConfiguration domain) {
    return new app.bpartners.geojobs.endpoint.rest.model.DetectableObjectConfiguration()
        .confidence(
            domain.getMinConfidenceForDetection() == null
                ? BigDecimal.valueOf(DEFAULT_CONFIDENCE_FOR_IN_DOUBT_DETECTION)
                : BigDecimal.valueOf(domain.getMinConfidenceForDetection()))
        .type(typeMapper.toRest(domain.getObjectType()))
        .bucketStorageName(domain.getBucketStorageName());
  }
}
