package app.bpartners.geojobs.service.detection;

import static app.bpartners.geojobs.service.detection.DetectionResponse.REGION_CONFIDENCE_PROPERTY;
import static app.bpartners.geojobs.service.detection.DetectionResponse.REGION_LABEL_PROPERTY;

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
  public DetectionResponse apply(
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

  private DetectionResponse aMockedDetectionResponse(
      Double confidence, DetectableType detectableType) {
    double randomX = new SecureRandom().nextDouble() * 100;
    double randomY = new SecureRandom().nextDouble() * 100;
    return DetectionResponse.builder()
        .rstImageUrl("dummyImageUrl")
        .srcImageUrl("dummyImageUrl")
        .rstRaw(
            Map.of(
                "dummyRstRawProperty",
                DetectionResponse.ImageData.builder()
                    .regions(
                        Map.of(
                            "dummyRegionProperty",
                            DetectionResponse.ImageData.Region.builder()
                                .regionAttributes(
                                    Map.of(
                                        REGION_CONFIDENCE_PROPERTY,
                                        confidence.toString(),
                                        REGION_LABEL_PROPERTY,
                                        detectableType.toString()))
                                .shapeAttributes(
                                    DetectionResponse.ImageData.ShapeAttributes.builder()
                                        .allPointsX(List.of(BigDecimal.valueOf(randomX)))
                                        .allPointsY(List.of(BigDecimal.valueOf(randomY)))
                                        .build())
                                .build()))
                    .build()))
        .build();
  }
}
