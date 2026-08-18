package app.bpartners.geojobs.unit;

import static app.bpartners.geojobs.endpoint.rest.model.ModelName.STATIONNEMENT;
import static app.bpartners.geojobs.endpoint.rest.model.ModelName.TOITURE;
import static app.bpartners.geojobs.endpoint.rest.model.MultiPolygon.TypeEnum.MULTI_POLYGON;
import static app.bpartners.geojobs.postprocessing.StationnementPostProcessor.AREA_IN_SQUARE_METER_PROPERTY;
import static app.bpartners.geojobs.postprocessing.StationnementPostProcessor.MIN_PLACE_STANDARD_AREA_IN_SQUARE_METER;
import static app.bpartners.geojobs.service.geojson.GeoJson.fromFeatures;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import app.bpartners.geojobs.endpoint.rest.model.DetectableObjectModel;
import app.bpartners.geojobs.endpoint.rest.model.ModelName;
import app.bpartners.geojobs.postprocessing.StationnementPostProcessor;
import app.bpartners.geojobs.repository.model.detection.Detection;
import app.bpartners.geojobs.service.GeometrySquareMeterArea;
import app.bpartners.geojobs.service.geojson.GeoJson;
import app.bpartners.geojobs.service.geojson.GeometryConverter;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Geometry;

class StationnementPostProcessorTest {
  private static final String RESULT_GEOJSON =
      "/stationnement_toiture/Result_BP_TOITURE_+BP_STATIONNEMENT.geojson";
  private static final String GROUND_TRUTH_GEOJSON =
      "/stationnement_toiture/Ground_truth_BP_TOITURE_+BP_STATIONNEMENT.geojson";
  private static final String PLACE_STANDARD_LABEL = "PLACE_STANDARD";
  private static final String PARKING_LABEL = "PARKING";
  private static final double SELF_INTERSECTING_REPAIRED_AREA = 13.05;
  private static final double AREA_TOLERANCE = 0.01;

  private final ObjectMapper objectMapper = new ObjectMapper();
  private final GeometryConverter geometryConverter = new GeometryConverter();
  private final GeometrySquareMeterArea geometrySquareMeterArea = new GeometrySquareMeterArea();
  private final StationnementPostProcessor subject =
      new StationnementPostProcessor(geometryConverter, geometrySquareMeterArea);

  @Test
  void filters_out_noisy_place_standard_objects_of_stationnement_model() throws IOException {
    var detectedPlaceStandard = labeledGeoFeatures(RESULT_GEOJSON, PLACE_STANDARD_LABEL);
    var manuallyRemovedGeometries = new HashSet<>(geometriesOf(detectedPlaceStandard));
    manuallyRemovedGeometries.removeAll(
        geometriesOf(labeledGeoFeatures(GROUND_TRUTH_GEOJSON, PLACE_STANDARD_LABEL)));

    var actual = subject.apply(fromFeatures(detectedPlaceStandard), detection(STATIONNEMENT));

    assertFalse(manuallyRemovedGeometries.isEmpty(), "noisy objects are expected in the fixture");
    var actualGeometries = new HashSet<>(geometriesOf(actual.getGeoFeatures()));
    manuallyRemovedGeometries.forEach(
        noisyGeometry ->
            assertFalse(
                actualGeometries.contains(noisyGeometry),
                "noisy PLACE_STANDARD is expected to be filtered out"));
    detectedPlaceStandard.stream()
        .filter(
            geoFeature -> areaInSquareMeter(geoFeature) >= MIN_PLACE_STANDARD_AREA_IN_SQUARE_METER)
        .forEach(
            wideEnough ->
                assertTrue(
                    actualGeometries.contains(geometryOf(wideEnough)),
                    "PLACE_STANDARD wide enough is expected to be kept"));
  }

  @Test
  void delivers_the_area_of_kept_place_standard_objects() throws IOException {
    var detectedPlaceStandard = labeledGeoFeatures(RESULT_GEOJSON, PLACE_STANDARD_LABEL);

    var actual = subject.apply(fromFeatures(detectedPlaceStandard), detection(STATIONNEMENT));

    assertFalse(actual.getGeoFeatures().isEmpty(), "kept objects are expected in the fixture");
    actual
        .getGeoFeatures()
        .forEach(
            geoFeature ->
                assertEquals(
                    areaInSquareMeter(geoFeature),
                    geoFeature.getProperties().get(AREA_IN_SQUARE_METER_PROPERTY),
                    "kept PLACE_STANDARD is expected to carry its own area"));
  }

  @Test
  void delivers_the_area_in_the_serialized_geo_json() throws IOException {
    var detectedPlaceStandard = labeledGeoFeatures(RESULT_GEOJSON, PLACE_STANDARD_LABEL);

    var actual = subject.apply(fromFeatures(detectedPlaceStandard), detection(STATIONNEMENT));

    var serializedProperties = serializedPropertiesOf(actual);
    assertFalse(serializedProperties.isEmpty(), "kept objects are expected in the fixture");
    assertEquals(actual.getGeoFeatures().size(), serializedProperties.size());
    for (int i = 0; i < serializedProperties.size(); i++) {
      var properties = serializedProperties.get(i);
      assertTrue(
          properties.hasNonNull(AREA_IN_SQUARE_METER_PROPERTY),
          "area is expected in the delivered geojson, not only in memory");
      assertEquals(
          areaInSquareMeter(actual.getGeoFeatures().get(i)),
          properties.get(AREA_IN_SQUARE_METER_PROPERTY).asDouble());
    }
  }

  @Test
  void delivers_the_area_even_when_no_object_is_filtered_out() throws IOException {
    var wideEnoughOnly =
        labeledGeoFeatures(RESULT_GEOJSON, PLACE_STANDARD_LABEL).stream()
            .filter(
                geoFeature ->
                    areaInSquareMeter(geoFeature) >= MIN_PLACE_STANDARD_AREA_IN_SQUARE_METER)
            .toList();

    var actual = subject.apply(fromFeatures(wideEnoughOnly), detection(STATIONNEMENT));

    assertEquals(wideEnoughOnly.size(), actual.getGeoFeatures().size(), "nothing to filter out");
    serializedPropertiesOf(actual)
        .forEach(
            properties ->
                assertTrue(
                    properties.hasNonNull(AREA_IN_SQUARE_METER_PROPERTY),
                    "area is expected even when no noisy object was filtered out"));
  }

  @Test
  void delivers_the_area_of_parking_objects_without_filtering_them_out() throws IOException {
    var detectedParking = labeledGeoFeatures(RESULT_GEOJSON, PARKING_LABEL);

    var actual = subject.apply(fromFeatures(detectedParking), detection(STATIONNEMENT));

    assertFalse(detectedParking.isEmpty(), "PARKING objects are expected in the fixture");
    assertEquals(detectedParking.size(), actual.getGeoFeatures().size(), "PARKING is never noise");
    actual
        .getGeoFeatures()
        .forEach(
            geoFeature ->
                assertEquals(
                    areaInSquareMeter(geoFeature),
                    geoFeature.getProperties().get(AREA_IN_SQUARE_METER_PROPERTY),
                    "PARKING is expected to carry its own area"));
    serializedPropertiesOf(actual)
        .forEach(
            properties ->
                assertTrue(
                    properties.hasNonNull(AREA_IN_SQUARE_METER_PROPERTY),
                    "area is expected in the delivered geojson, not only in memory"));
  }

  @Test
  void delivers_the_area_of_place_standard_and_parking_objects_only() throws IOException {
    var mixedDetected =
        Stream.of(PLACE_STANDARD_LABEL, PARKING_LABEL, "OBSTACLE", "CHEMINEE")
            .map(this::labeledGeoFeaturesOfResult)
            .flatMap(List::stream)
            .toList();

    var actual = subject.apply(fromFeatures(mixedDetected), detection(STATIONNEMENT));

    var labelsWithArea =
        actual.getGeoFeatures().stream()
            .filter(
                geoFeature -> geoFeature.getProperties().get(AREA_IN_SQUARE_METER_PROPERTY) != null)
            .map(geoFeature -> geoFeature.getProperties().get("label"))
            .collect(HashSet::new, Set::add, Set::addAll);

    assertEquals(Set.of(PLACE_STANDARD_LABEL, PARKING_LABEL), labelsWithArea);
  }

  @Test
  void does_not_deliver_any_area_for_objects_other_than_place_standard_and_parking()
      throws IOException {
    var detectedObstacle = labeledGeoFeatures(RESULT_GEOJSON, "OBSTACLE");

    var actual = subject.apply(fromFeatures(detectedObstacle), detection(STATIONNEMENT));

    assertFalse(detectedObstacle.isEmpty(), "OBSTACLE objects are expected in the fixture");
    actual
        .getGeoFeatures()
        .forEach(
            geoFeature ->
                assertNull(geoFeature.getProperties().get(AREA_IN_SQUARE_METER_PROPERTY)));
  }

  @Test
  void repairs_the_self_intersecting_geometry_of_a_parking() {
    var selfIntersectingParking = selfIntersectingGeoFeature(PARKING_LABEL);
    assertFalse(jtsGeometryOf(selfIntersectingParking).isValid(), "fixture is self-intersecting");

    var actual =
        subject.apply(fromFeatures(List.of(selfIntersectingParking)), detection(STATIONNEMENT));

    var repaired = actual.getGeoFeatures().getFirst();
    assertTrue(jtsGeometryOf(repaired).isValid(), "the delivered geometry is expected to be valid");
    assertEquals(
        SELF_INTERSECTING_REPAIRED_AREA,
        (Double) repaired.getProperties().get(AREA_IN_SQUARE_METER_PROPERTY),
        AREA_TOLERANCE,
        "the area is expected to be the sum of the self-intersecting parts");
  }

  @Test
  void keeps_a_place_standard_whose_self_intersecting_geometry_is_wide_enough_once_repaired() {
    var selfIntersectingPlaceStandard = selfIntersectingGeoFeature(PLACE_STANDARD_LABEL);
    assertTrue(
        rawAreaInSquareMeter(selfIntersectingPlaceStandard)
            < MIN_PLACE_STANDARD_AREA_IN_SQUARE_METER,
        "raw area is expected to look like noise");
    assertTrue(
        SELF_INTERSECTING_REPAIRED_AREA > MIN_PLACE_STANDARD_AREA_IN_SQUARE_METER,
        "repaired area is expected to be wide enough");

    var actual =
        subject.apply(
            fromFeatures(List.of(selfIntersectingPlaceStandard)), detection(STATIONNEMENT));

    assertEquals(1, actual.getGeoFeatures().size(), "it is an actual place, not noise");
    var repaired = actual.getGeoFeatures().getFirst();
    assertTrue(jtsGeometryOf(repaired).isValid(), "the delivered geometry is expected to be valid");
    assertEquals(
        SELF_INTERSECTING_REPAIRED_AREA,
        (Double) repaired.getProperties().get(AREA_IN_SQUARE_METER_PROPERTY),
        AREA_TOLERANCE);
  }

  @Test
  void repairs_the_geometry_in_the_serialized_geo_json() {
    var selfIntersectingParking = selfIntersectingGeoFeature(PARKING_LABEL);

    var actual =
        subject.apply(fromFeatures(List.of(selfIntersectingParking)), detection(STATIONNEMENT));

    var deliveredGeoFeatures = deserializedGeoFeaturesOf(actual);
    assertEquals(1, deliveredGeoFeatures.size());
    assertTrue(
        jtsGeometryOf(deliveredGeoFeatures.getFirst()).isValid(),
        "the repaired geometry is expected in the delivered geojson, not only in memory");
  }

  @Test
  void delivers_an_unusable_geometry_untouched_and_without_area() {
    var unusable =
        geoFeature(PARKING_LABEL, List.of(List.of(List.of(point(2.0, 48.0), point(2.0, 48.0)))));

    var actual = subject.apply(fromFeatures(List.of(unusable)), detection(STATIONNEMENT));

    assertEquals(1, actual.getGeoFeatures().size(), "it is not filtered out on an unknown area");
    var delivered = actual.getGeoFeatures().getFirst();
    assertNull(delivered.getProperties().get(AREA_IN_SQUARE_METER_PROPERTY));
    assertEquals(unusable.getGeometry().getCoordinates(), delivered.getGeometry().getCoordinates());
  }

  @Test
  void does_not_filter_out_anything_when_model_is_not_stationnement() throws IOException {
    var detectedPlaceStandard = labeledGeoFeatures(RESULT_GEOJSON, PLACE_STANDARD_LABEL);
    var geoJson = fromFeatures(detectedPlaceStandard);

    assertEquals(geoJson, subject.apply(geoJson, detection(TOITURE)));
    assertEquals(geoJson, subject.apply(geoJson, null));
  }

  @Test
  void does_not_filter_out_objects_other_than_place_standard() throws IOException {
    var detectedParking = labeledGeoFeatures(RESULT_GEOJSON, PARKING_LABEL);
    var geoJson = fromFeatures(detectedParking);

    var actual = subject.apply(geoJson, detection(STATIONNEMENT));

    assertEquals(detectedParking.size(), actual.getGeoFeatures().size());
  }

  private GeoJson.GeoFeature selfIntersectingGeoFeature(String label) {
    return geoFeature(
        label,
        List.of(
            List.of(
                List.of(
                    point(2.0, 48.0),
                    point(2.00007, 48.000045),
                    point(2.0, 48.000045),
                    point(2.00007, 48.0),
                    point(2.0, 48.0)))));
  }

  private GeoJson.GeoFeature geoFeature(
      String label, List<List<List<List<BigDecimal>>>> coordinates) {
    Map<String, Object> properties = new HashMap<>();
    properties.put("label", label);
    return new GeoJson.GeoFeature(
        properties,
        new app.bpartners.geojobs.endpoint.rest.model.MultiPolygon()
            .type(MULTI_POLYGON)
            .coordinates(coordinates));
  }

  private List<BigDecimal> point(double x, double y) {
    return List.of(BigDecimal.valueOf(x), BigDecimal.valueOf(y));
  }

  private Geometry jtsGeometryOf(GeoJson.GeoFeature geoFeature) {
    return geometryConverter.apply(geoFeature.getGeometry().getCoordinates());
  }

  private double rawAreaInSquareMeter(GeoJson.GeoFeature geoFeature) {
    return geometrySquareMeterArea.apply(jtsGeometryOf(geoFeature));
  }

  @SneakyThrows
  private List<GeoJson.GeoFeature> deserializedGeoFeaturesOf(GeoJson geoJson) {
    var root = objectMapper.readTree(geoJson.getStringValue());
    var features = root.isArray() ? root : root.get("features");
    List<GeoJson.GeoFeature> geoFeatures = new ArrayList<>();
    for (JsonNode feature : features) {
      geoFeatures.add(objectMapper.treeToValue(feature, GeoJson.GeoFeature.class));
    }
    return geoFeatures;
  }

  private Detection detection(ModelName modelName) {
    return Detection.builder()
        .id("detection_id")
        .detectableObjectModelList(List.of(new DetectableObjectModel().modelName(modelName)))
        .build();
  }

  private double areaInSquareMeter(GeoJson.GeoFeature geoFeature) {
    return geometrySquareMeterArea.apply(
        geometryConverter.apply(geoFeature.getGeometry().getCoordinates()));
  }

  private List<JsonNode> serializedPropertiesOf(GeoJson geoJson) throws IOException {
    var root = objectMapper.readTree(geoJson.getStringValue());
    var features = root.isArray() ? root : root.get("features");
    List<JsonNode> properties = new ArrayList<>();
    for (JsonNode feature : features) {
      properties.add(feature.get("properties"));
    }
    return properties;
  }

  private Set<String> geometriesOf(List<GeoJson.GeoFeature> geoFeatures) {
    return geoFeatures.stream().map(this::geometryOf).collect(HashSet::new, Set::add, Set::addAll);
  }

  private String geometryOf(GeoJson.GeoFeature geoFeature) {
    return geometryConverter.apply(geoFeature.getGeometry().getCoordinates()).toText();
  }

  @SneakyThrows
  private List<GeoJson.GeoFeature> labeledGeoFeaturesOfResult(String label) {
    return labeledGeoFeatures(RESULT_GEOJSON, label);
  }

  private List<GeoJson.GeoFeature> labeledGeoFeatures(String resourcePath, String label)
      throws IOException {
    try (var geoJsonStream = getClass().getResourceAsStream(resourcePath)) {
      assertNotNull(geoJsonStream, "test resource not found on classpath: " + resourcePath);
      JsonNode featureCollection = objectMapper.readTree(geoJsonStream);
      List<GeoJson.GeoFeature> geoFeatures = new ArrayList<>();
      for (JsonNode feature : featureCollection.get("features")) {
        var properties = feature.get("properties");
        if (properties == null
            || properties.get("label") == null
            || !label.equals(properties.get("label").asText())) {
          continue;
        }
        geoFeatures.add(objectMapper.treeToValue(feature, GeoJson.GeoFeature.class));
      }
      return geoFeatures;
    }
  }
}
