package app.bpartners.geojobs.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import app.bpartners.geojobs.service.detection.DetectionResponse;
import app.bpartners.geojobs.service.detection.DetectionResponseV1ToV2Mapper;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class DetectionResponseV1ToV2MapperTest {
  DetectionResponseV1ToV2Mapper subject = new DetectionResponseV1ToV2Mapper();

  @Test
  void returns_null_for_null_input() {
    assertNull(subject.apply(null));
  }

  @Test
  void folds_rst_raw_and_vegetation_into_images() {
    var v1 =
        DetectionResponse.builder()
            .rstRaw(Map.of("383095.jpg", imageData("roof_tuiles")))
            .vegetationData(imageData("green_space"))
            .build();

    var actual = subject.apply(v1);

    assertNotNull(actual);
    assertEquals(2, actual.getImages().size());
    assertTrue(actual.getImages().containsKey("383095.jpg"));
    assertTrue(
        actual.getImages().keySet().stream().anyMatch(k -> k.startsWith("vegetation_data_")));
    var roofImage = actual.getImages().get("383095.jpg");
    var region = roofImage.getRegions().values().iterator().next();
    assertEquals("roof_tuiles", region.getRegionAttributes().get("label"));
    assertEquals(List.of(BigDecimal.ONE), region.getShapeAttributes().getAllPointsX());
  }

  private DetectionResponse.ImageData imageData(String label) {
    return DetectionResponse.ImageData.builder()
        .filename("file")
        .regions(
            Map.of(
                "0",
                DetectionResponse.ImageData.Region.builder()
                    .regionAttributes(new java.util.HashMap<>(Map.of("label", label)))
                    .shapeAttributes(
                        DetectionResponse.ImageData.ShapeAttributes.builder()
                            .name("polygon")
                            .allPointsX(List.of(BigDecimal.ONE))
                            .allPointsY(List.of(BigDecimal.TEN))
                            .build())
                    .build()))
        .build();
  }
}
