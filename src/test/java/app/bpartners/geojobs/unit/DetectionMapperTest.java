package app.bpartners.geojobs.unit;

import static app.bpartners.geojobs.endpoint.rest.controller.v1.mapper.FeatureMapper.toRestFeature;
import static app.bpartners.geojobs.model.CustomObjectMapper.objectMapper;
import static app.bpartners.geojobs.repository.model.detection.DetectableType.PISCINE;
import static app.bpartners.geojobs.repository.model.detection.ZoneDetectionJob.DetectionType.HUMAN;
import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import app.bpartners.gen.annotator.endpoint.rest.model.Annotation;
import app.bpartners.gen.annotator.endpoint.rest.model.Label;
import app.bpartners.gen.annotator.endpoint.rest.model.Point;
import app.bpartners.gen.annotator.endpoint.rest.model.Polygon;
import app.bpartners.geojobs.endpoint.rest.model.Geometry;
import app.bpartners.geojobs.endpoint.rest.model.MultiPolygon;
import app.bpartners.geojobs.endpoint.rest.model.TileCoordinates;
import app.bpartners.geojobs.repository.model.Feature;
import app.bpartners.geojobs.repository.model.tiling.Tile;
import app.bpartners.geojobs.service.detection.DetectionMapper;
import app.bpartners.geojobs.service.detection.DetectionResponseV2;
import app.bpartners.geojobs.service.tiling.TileValidator;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;

class DetectionMapperTest {
  TileValidator tileValidator = new TileValidator();
  DetectionMapper subject = new DetectionMapper(tileValidator);

  DetectionResponseV2 detectionResponse() {
    return DetectionResponseV2.builder().images(Map.of("filename", imageData())).build();
  }

  DetectionResponseV2.ImageData imageData() {
    return DetectionResponseV2.ImageData.builder()
        .base64ImgData("image_base_64")
        .regions(Map.of("regions", region()))
        .build();
  }

  DetectionResponseV2.ImageData.Region region() {
    return DetectionResponseV2.ImageData.Region.builder()
        .regionAttributes(Map.of("label", "PISCINE", "confidence", "0.95"))
        .shapeAttributes(shapes())
        .build();
  }

  DetectionResponseV2.ImageData.ShapeAttributes shapes() {
    return DetectionResponseV2.ImageData.ShapeAttributes.builder()
        .allPointsX(
            List.of(BigDecimal.valueOf(6.958009303660302), BigDecimal.valueOf(43.543013820437459)))
        .allPointsY(
            List.of(BigDecimal.valueOf(6.957965493371299), BigDecimal.valueOf(43.543002082885863)))
        .build();
  }

  @Test
  void map_to_detection_response_to_detected_tile() {
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

  @Test
  void map_annotation_to_human_detected_object() {
    var zoom = 20;
    var tileId = randomUUID().toString();

    var actual = subject.toHumanDetectedObject(zoom, tileId, List.of(annotation()));
    var detectedObject = actual.getFirst();

    assertFalse(actual.isEmpty());
    assertEquals(PISCINE, detectedObject.getDetectableObjectType());
    assertEquals(HUMAN, detectedObject.getType());
    assertEquals(tileId, detectedObject.getDetectedTileId());
    assertEquals(0.9515481, detectedObject.getComputedConfidence());
    Map<String, Object> properties = feature().getProperties();
    properties.put("zoom", zoom);
    properties.put("id", detectedObject.getFeature().getProperties().get("id"));
    assertEquals(feature().properties(properties), detectedObject.getFeature());
  }

  private Annotation annotation() {
    return new Annotation()
        .comment("confidence=95.15481")
        .label(new Label().id(randomUUID().toString()).name("PISCINE").color("#fffff"))
        .polygon(new Polygon().points(List.of(new Point().y(500.0).x(147.5))));
  }

  @SneakyThrows
  private app.bpartners.geojobs.endpoint.rest.model.Feature feature() {
    var coordinates =
        List.of(List.of(List.of(List.of(new BigDecimal("147.5"), new BigDecimal("500.0")))));
    return toRestFeature(
        Feature.builder()
            .id(null)
            .zoom(20)
            .geometry(
                Feature.FeatureGeometry.builder()
                    .geometryType(Geometry.TypeEnum.MULTI_POLYGON)
                    .actualInstanceStringValue(
                        objectMapper()
                            .writeValueAsString(
                                new MultiPolygon()
                                    .coordinates(coordinates)
                                    .type(MultiPolygon.TypeEnum.MULTI_POLYGON)))
                    .build())
            .properties(new HashMap<>())
            .build());
  }
}
