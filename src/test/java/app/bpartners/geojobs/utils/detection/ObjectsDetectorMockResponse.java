package app.bpartners.geojobs.utils.detection;

import static app.bpartners.geojobs.model.exception.ApiException.ExceptionType.SERVER_EXCEPTION;
import static app.bpartners.geojobs.service.detection.DetectionResponseV2.REGION_CONFIDENCE_PROPERTY;
import static app.bpartners.geojobs.service.detection.DetectionResponseV2.REGION_LABEL_PROPERTY;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;

import app.bpartners.geojobs.model.exception.ApiException;
import app.bpartners.geojobs.service.detection.DetectionResponseV2;
import app.bpartners.geojobs.service.detection.TileObjectDetector;
import java.math.BigDecimal;
import java.security.SecureRandom;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class ObjectsDetectorMockResponse {
  private final TileObjectDetector objectsDetector;

  public ObjectsDetectorMockResponse(TileObjectDetector objectsDetector) {
    this.objectsDetector = objectsDetector;
  }

  public void apply(double responseConfidence, String objectType, double successRate) {
    Random random = new Random();
    doAnswer(
            invocation -> {
              double randomDouble = random.nextDouble() * 100;
              if (randomDouble < successRate) {
                return aDetectionResponse(responseConfidence, objectType);
              } else {
                throw new ApiException(SERVER_EXCEPTION, "Server error");
              }
            })
        .when(objectsDetector)
        .apply(any(), any(), any());
  }

  private DetectionResponseV2 aDetectionResponse(Double confidence, String objectType) {
    double randomX = new SecureRandom().nextDouble() * 100;
    double randomY = new SecureRandom().nextDouble() * 100;
    return DetectionResponseV2.builder()
        .images(
            Map.of(
                "dummyImageProperty",
                DetectionResponseV2.ImageData.builder()
                    .regions(
                        Map.of(
                            "dummyRegionProperty",
                            DetectionResponseV2.ImageData.Region.builder()
                                .regionAttributes(
                                    Map.of(
                                        REGION_CONFIDENCE_PROPERTY,
                                        confidence.toString(),
                                        REGION_LABEL_PROPERTY,
                                        objectType))
                                .shapeAttributes(
                                    DetectionResponseV2.ImageData.ShapeAttributes.builder()
                                        .allPointsX(List.of(BigDecimal.valueOf(randomX)))
                                        .allPointsY(List.of(BigDecimal.valueOf(randomY)))
                                        .build())
                                .build()))
                    .build()))
        .build();
  }
}
