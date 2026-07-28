package app.bpartners.geojobs.unit;

import static app.bpartners.geojobs.repository.model.detection.DetectableType.LINE;
import static app.bpartners.geojobs.repository.model.detection.DetectableType.TOITURE_REVETEMENT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

import app.bpartners.geojobs.endpoint.rest.controller.v1.mapper.DetectableObjectConfigurationMapper;
import app.bpartners.geojobs.endpoint.rest.controller.v1.mapper.DetectableObjectTypeMapper;
import app.bpartners.geojobs.endpoint.rest.model.DetectableObjectType;
import app.bpartners.geojobs.file.bucket.BucketConf;
import app.bpartners.geojobs.repository.model.detection.DetectableObjectConfiguration;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class DetectableObjectConfigurationMapperTest {
  BucketConf bucketConf = mock();
  DetectableObjectConfigurationMapper subject =
      new DetectableObjectConfigurationMapper(new DetectableObjectTypeMapper(), bucketConf);

  @Test
  void to_rest_ok() {
    var dummyBucket = "dummyBucket";
    var expected1 =
        new app.bpartners.geojobs.endpoint.rest.model.DetectableObjectConfiguration()
            .confidence(BigDecimal.valueOf(0.5))
            .type(DetectableObjectType.TOITURE_REVETEMENT)
            .bucketStorageName(dummyBucket);
    var expected2 =
        new app.bpartners.geojobs.endpoint.rest.model.DetectableObjectConfiguration()
            .confidence(BigDecimal.valueOf(1.0))
            .type(DetectableObjectType.LINE)
            .bucketStorageName(dummyBucket);

    var actual1 =
        subject.toRest(
            DetectableObjectConfiguration.builder()
                .minConfidenceForDetection(0.5)
                .objectType(TOITURE_REVETEMENT)
                .bucketStorageName(dummyBucket)
                .build());
    var actual2 =
        subject.toRest(
            DetectableObjectConfiguration.builder()
                .minConfidenceForDetection(null)
                .objectType(LINE)
                .bucketStorageName(dummyBucket)
                .build());

    assertEquals(expected1, actual1);
    assertEquals(expected2, actual2);
  }
}
