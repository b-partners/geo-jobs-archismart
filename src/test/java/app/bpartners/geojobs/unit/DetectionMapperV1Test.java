package app.bpartners.geojobs.unit;

import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import app.bpartners.geojobs.endpoint.rest.model.TileCoordinates;
import app.bpartners.geojobs.repository.model.tiling.Tile;
import app.bpartners.geojobs.service.detection.DetectionMapperV1;
import app.bpartners.geojobs.service.detection.DetectionResponse;
import app.bpartners.geojobs.service.tiling.TileValidator;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class DetectionMapperV1Test {
  TileValidator tileValidator = new TileValidator();
  DetectionMapperV1 subject = new DetectionMapperV1(tileValidator);

  DetectionResponse detectionResponse() {
    return DetectionResponse.builder().rstRaw(Map.of("filename", imageData())).build();
  }

  DetectionResponse.ImageData imageData() {
    return DetectionResponse.ImageData.builder()
        .base64ImgData("image_base_64")
        .regions(Map.of("regions", region()))
        .build();
  }

  DetectionResponse.ImageData.Region region() {
    return DetectionResponse.ImageData.Region.builder()
        .regionAttributes(Map.of("label", "PISCINE", "confidence", "0.95"))
        .shapeAttributes(shapes())
        .build();
  }

  DetectionResponse.ImageData.ShapeAttributes shapes() {
    return DetectionResponse.ImageData.ShapeAttributes.builder()
        .allPointsX(
            List.of(BigDecimal.valueOf(6.958009303660302), BigDecimal.valueOf(43.543013820437459)))
        .allPointsY(
            List.of(BigDecimal.valueOf(6.957965493371299), BigDecimal.valueOf(43.543002082885863)))
        .build();
  }

  @Test
  void map_v1_detection_response_to_detected_tile() {
    var parcelId = randomUUID().toString();
    var zoneJobId = randomUUID().toString();
    var parcelJobId = randomUUID().toString();
    var tile =
        Tile.builder()
            .id("tile_id")
            .coordinates(new TileCoordinates().x(5000).y(2000).z(20))
            .build();
    var actual =
        subject.toDetectedTile(detectionResponse(), tile, parcelId, zoneJobId, parcelJobId);

    assertNotNull(actual);
    assertFalse(actual.getDetectedObjects().isEmpty());
    assertEquals(tile, actual.getTile());
    assertEquals(parcelId, actual.getParcelId());
    assertEquals(zoneJobId, actual.getZdjJobId());
    assertEquals(parcelJobId, actual.getParcelJobId());
  }
}
