package app.bpartners.geojobs.service;

import static app.bpartners.geojobs.endpoint.rest.model.Geometry.TypeEnum.MULTI_POLYGON;
import static app.bpartners.geojobs.model.CustomObjectMapper.objectMapper;
import static app.bpartners.geojobs.repository.model.detection.DetectableType.PISCINE;
import static app.bpartners.geojobs.repository.model.detection.ZoneDetectionJob.DetectionType.MACHINE;
import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import app.bpartners.geojobs.endpoint.rest.model.MultiPolygon;
import app.bpartners.geojobs.endpoint.rest.model.TileCoordinates;
import app.bpartners.geojobs.model.DetectedTile;
import app.bpartners.geojobs.repository.model.Feature;
import app.bpartners.geojobs.repository.model.detection.DetectableObjectType;
import app.bpartners.geojobs.repository.model.detection.DetectableType;
import app.bpartners.geojobs.repository.model.detection.DetectedObject;
import app.bpartners.geojobs.repository.model.tiling.Tile;
import com.fasterxml.jackson.databind.JsonNode;
import java.io.File;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;

class DetectedTileVggExtractorTest {
  private final DetectedTileVggExtractor subject = new DetectedTileVggExtractor();

  private static final List<BigDecimal> POINTS_X =
      List.of(BigDecimal.TEN, BigDecimal.valueOf(100), BigDecimal.valueOf(50));
  private static final List<BigDecimal> POINTS_Y =
      List.of(BigDecimal.TEN, BigDecimal.TEN, BigDecimal.valueOf(100));

  @SneakyThrows
  private Feature pixelFeature(String address) {
    var ring = new java.util.ArrayList<List<BigDecimal>>();
    for (int i = 0; i < POINTS_X.size(); i++) {
      ring.add(List.of(POINTS_X.get(i), POINTS_Y.get(i)));
    }
    HashMap<String, Object> properties = new HashMap<>();
    if (address != null) {
      properties.put("address", address);
    }
    return Feature.builder()
        .id(randomUUID().toString())
        .zoom(20)
        .geometry(
            Feature.FeatureGeometry.builder()
                .geometryType(MULTI_POLYGON)
                .actualInstanceStringValue(
                    objectMapper()
                        .writeValueAsString(
                            new MultiPolygon()
                                .type(MultiPolygon.TypeEnum.MULTI_POLYGON)
                                .coordinates(List.of(List.of(ring)))))
                .build())
        .properties(properties)
        .build();
  }

  private DetectedObject detectedObject(DetectableType type, Double confidence, String address) {
    var objectId = randomUUID().toString();
    return DetectedObject.builder()
        .id(objectId)
        .type(MACHINE)
        .detectedObjectType(
            DetectableObjectType.builder()
                .id(randomUUID().toString())
                .objectId(objectId)
                .detectableType(type)
                .build())
        .feature(pixelFeature(address))
        .computedConfidence(confidence)
        .build();
  }

  private DetectedTile detectedTile(List<DetectedObject> detectedObjects) {
    return DetectedTile.builder()
        .tile(
            Tile.builder()
                .id("tile_id")
                .bucketPath("tiles/zone/5000_2000_20.jpg")
                .coordinates(new TileCoordinates().x(5000).y(2000).z(20))
                .build())
        .detectedObjects(detectedObjects)
        .build();
  }

  @Test
  void builds_one_pixel_region_per_detected_object() {
    var tile = detectedTile(List.of(detectedObject(PISCINE, 0.95, "12 rue de la paix")));

    var vgg = subject.apply(tile);

    assertEquals(1, vgg.size());
    var annotation = vgg.get("5000_2000_20.jpg"); // filename derived from the tile bucket path
    assertNotNull(annotation);
    assertEquals(1, annotation.getRegions().size());

    var region = annotation.getRegions().values().iterator().next();
    var shape = region.getShapeAttribute();
    assertEquals("Polygon", shape.getName());
    assertEquals(List.of(10.0, 100.0, 50.0, 10.0), shape.getAllPointsX());
    assertEquals(List.of(10.0, 10.0, 100.0, 10.0), shape.getAllPointsY());
    assertEquals("PISCINE", region.getRegionAttribute().get("label"));
    assertEquals(0.95, region.getRegionAttribute().get("confidence"));
    assertEquals(List.of("12 rue de la paix"), annotation.getProperties().get("addresses"));
  }

  @Test
  void aggregates_regions_and_distinct_addresses_from_all_objects() {
    var tile =
        detectedTile(
            List.of(
                detectedObject(PISCINE, 0.9, "same address"),
                detectedObject(DetectableType.ARBRE, 0.8, "same address")));

    var vgg = subject.apply(tile);

    var annotation = vgg.values().iterator().next();
    assertEquals(2, annotation.getRegions().size());
    assertEquals(List.of("same address"), annotation.getProperties().get("addresses"));
  }

  @Test
  void empty_tile_yields_an_annotation_without_region() {
    var vgg = subject.apply(detectedTile(List.of()));

    var annotation = vgg.values().iterator().next();
    assertTrue(annotation.getRegions().isEmpty());
    assertTrue(annotation.getProperties().isEmpty());
  }

  @Test
  @SneakyThrows
  void temp_file_is_a_json_array_consumable_by_the_annotator() {
    var tile = detectedTile(List.of(detectedObject(PISCINE, 0.95, null)));

    File tempFile = subject.applyAsTempFile(tile);

    assertTrue(tempFile.exists() && tempFile.length() > 0);
    JsonNode root = objectMapper().readTree(tempFile);
    // VggImageAnnotator requires the VGG file to be a JSON array of {key: annotation} objects
    assertTrue(root.isArray());
    assertEquals(1, root.size());
    assertTrue(root.get(0).has("5000_2000_20.jpg"));
  }
}
