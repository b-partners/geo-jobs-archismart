package app.bpartners.geojobs.service.detection;

import static app.bpartners.geojobs.service.detection.DetectionResponseV2.REGION_CONFIDENCE_PROPERTY;
import static app.bpartners.geojobs.service.detection.DetectionResponseV2.REGION_LABEL_PROPERTY;

import app.bpartners.geojobs.repository.model.TileDetectionTask;
import app.bpartners.geojobs.repository.model.detection.DetectableObjectConfiguration;
import app.bpartners.geojobs.repository.model.detection.DetectableType;
import java.io.File;
import java.math.BigDecimal;
import java.security.SecureRandom;
import java.util.List;
import java.util.Map;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(value = "objects.detector.mock.activated", havingValue = "true")
public class MockedTileObjectDetector implements TileObjectDetector {
  @Override
  public DetectionResponseV2 apply(
      TileDetectionTask tileDetectionTask,
      File mask,
      List<DetectableObjectConfiguration> detectableObjectConfigurations) {
    double randomConfidence = new SecureRandom().nextDouble();
    var detectableTypes =
        detectableObjectConfigurations.stream()
            .map(DetectableObjectConfiguration::getObjectType)
            .toList();
    return aMockedDetectionResponse(
        randomConfidence,
        detectableTypes.isEmpty() ? DetectableType.TOITURE_REVETEMENT : detectableTypes.getFirst());
  }

  private DetectionResponseV2 aMockedDetectionResponse(
      Double confidence, DetectableType detectableType) {
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
                                        detectableType.toString()))
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
