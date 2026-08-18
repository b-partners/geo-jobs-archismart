package app.bpartners.geojobs.unit;

import static app.bpartners.geojobs.endpoint.rest.model.Geometry.TypeEnum.MULTI_POLYGON;
import static app.bpartners.geojobs.model.CustomObjectMapper.objectMapper;
import static app.bpartners.geojobs.repository.model.detection.DetectableType.MOISISSURE_CLAIR;
import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import app.bpartners.geojobs.endpoint.rest.model.MultiPolygon;
import app.bpartners.geojobs.repository.model.Feature;
import app.bpartners.geojobs.repository.model.detection.DetectableObjectType;
import app.bpartners.geojobs.repository.model.detection.DetectedObject;
import app.bpartners.geojobs.service.geojson.GeoJson;
import app.bpartners.geojobs.service.geojson.GeoJsonMapper;
import app.bpartners.geojobs.service.geojson.GeoJsonMultiPolygonCorrector;
import java.math.BigDecimal;
import java.util.List;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;

class GeoJsonMapperTest {
  private final GeoJsonMapper subject = new GeoJsonMapper(new GeoJsonMultiPolygonCorrector());

  @SneakyThrows
  public static app.bpartners.geojobs.repository.model.Feature feature() {
    var coordinates =
        List.of(
            List.of(
                List.of(List.of(new BigDecimal("600.0"), new BigDecimal("136.5"))),
                List.of(List.of(new BigDecimal("566.0"), new BigDecimal("800.54"))),
                List.of(List.of(new BigDecimal("1022.0"), new BigDecimal("1010.0"))),
                List.of(List.of(new BigDecimal("6.0"), new BigDecimal("43.0")))));

    return Feature.builder()
        .geometry(
            Feature.FeatureGeometry.builder()
                .geometryType(MULTI_POLYGON)
                .actualInstanceStringValue(
                    objectMapper().writeValueAsString(new MultiPolygon().coordinates(coordinates)))
                .build())
        .build();
  }

  public static DetectedObject detectedObject() {
    return detectedObject(0.95);
  }

  public static DetectedObject detectedObject(Double computedConfidence) {
    return DetectedObject.builder()
        .id(randomUUID().toString())
        .feature(feature())
        .computedConfidence(computedConfidence)
        .detectedObjectType(DetectableObjectType.builder().detectableType(MOISISSURE_CLAIR).build())
        .build();
  }

  @Test
  void annotation_to_geo_json() {
    List<GeoJson.GeoFeature> actual =
        subject.toGeoFeatures(538559, 373791, 20, 1024, List.of(detectedObject()));

    assertNotNull(actual);
    assertFalse(actual.isEmpty());
  }

  @Test
  void confidence_property_is_mapped_as_double() {
    List<GeoJson.GeoFeature> actual =
        subject.toGeoFeatures(538559, 373791, 20, 1024, List.of(detectedObject(0.95)));

    var confidence = actual.getFirst().getProperties().get("confidence");
    assertInstanceOf(Double.class, confidence);
    assertEquals(0.95, confidence);
  }

  @Test
  void null_confidence_is_mapped_as_null() {
    List<GeoJson.GeoFeature> actual =
        subject.toGeoFeatures(538559, 373791, 20, 1024, List.of(detectedObject(null)));

    assertNull(actual.getFirst().getProperties().get("confidence"));
  }

  @Test
  void confidence_is_serialized_as_json_number() {
    List<GeoJson.GeoFeature> geoFeatures =
        subject.toGeoFeatures(538559, 373791, 20, 1024, List.of(detectedObject(0.95)));

    var actual = GeoJson.fromFeatures(geoFeatures).getStringValue();

    assertTrue(actual.contains("\"confidence\" : 0.95"), actual);
    assertFalse(actual.contains("\"confidence\" : \"0.95\""), actual);
  }
}
