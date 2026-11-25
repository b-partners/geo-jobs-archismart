package app.bpartners.geojobs.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import app.bpartners.geojobs.repository.model.Feature;
import app.bpartners.geojobs.service.dashboard.AnalysisProjectKeys;
import java.util.List;
import org.junit.jupiter.api.Test;

class AnalysisProjectKeysTest {
  static final String SQUARE = "[[[2.0,48.0],[2.001,48.0],[2.001,48.001],[2.0,48.001],[2.0,48.0]]]";

  @Test
  void key_is_prefixed_with_the_version() {
    var actual = AnalysisProjectKeys.of(List.of(polygon(SQUARE))).orElseThrow();

    assertTrue(actual.startsWith("v1:"));
  }

  @Test
  void same_polygon_whatever_its_start_point_gives_the_same_key() {
    var rotated = "[[[2.001,48.001],[2.0,48.001],[2.0,48.0],[2.001,48.0],[2.001,48.001]]]";

    assertEquals(
        AnalysisProjectKeys.of(List.of(polygon(SQUARE))),
        AnalysisProjectKeys.of(List.of(polygon(rotated))));
  }

  @Test
  void same_polygon_whatever_its_orientation_gives_the_same_key() {
    var reversed = "[[[2.0,48.0],[2.0,48.001],[2.001,48.001],[2.001,48.0],[2.0,48.0]]]";

    assertEquals(
        AnalysisProjectKeys.of(List.of(polygon(SQUARE))),
        AnalysisProjectKeys.of(List.of(polygon(reversed))));
  }

  @Test
  void coordinates_are_rounded_below_the_decimeter() {
    var noisy =
        "[[[2.00000004,48.00000003],[2.001000004,48.0],[2.001,48.001],[2.0,48.001],"
            + "[2.00000004,48.00000003]]]";

    assertEquals(
        AnalysisProjectKeys.of(List.of(polygon(SQUARE))),
        AnalysisProjectKeys.of(List.of(polygon(noisy))));
  }

  @Test
  void distinct_polygons_give_distinct_keys() {
    var shifted = "[[[2.01,48.0],[2.011,48.0],[2.011,48.001],[2.01,48.001],[2.01,48.0]]]";

    assertNotEquals(
        AnalysisProjectKeys.of(List.of(polygon(SQUARE))),
        AnalysisProjectKeys.of(List.of(polygon(shifted))));
  }

  @Test
  void feature_order_does_not_matter() {
    var other = "[[[2.01,48.0],[2.011,48.0],[2.011,48.001],[2.01,48.001],[2.01,48.0]]]";

    assertEquals(
        AnalysisProjectKeys.of(List.of(polygon(SQUARE), polygon(other))),
        AnalysisProjectKeys.of(List.of(polygon(other), polygon(SQUARE))));
  }

  @Test
  void multi_polygon_equals_its_polygons_provided_separately() {
    var other = "[[[2.01,48.0],[2.011,48.0],[2.011,48.001],[2.01,48.001],[2.01,48.0]]]";
    var multiPolygon =
        feature("{\"type\":\"MultiPolygon\",\"coordinates\":[" + SQUARE + "," + other + "]}");

    assertEquals(
        AnalysisProjectKeys.of(List.of(multiPolygon)),
        AnalysisProjectKeys.of(List.of(polygon(SQUARE), polygon(other))));
  }

  @Test
  void point_gives_a_stable_key() {
    var point = feature("{\"type\":\"Point\",\"coordinates\":[2.0,48.0]}");
    var samePoint = feature("{\"type\":\"Point\",\"coordinates\":[2.0000000001,48.0]}");

    assertEquals(
        AnalysisProjectKeys.of(List.of(point)), AnalysisProjectKeys.of(List.of(samePoint)));
    assertNotEquals(
        AnalysisProjectKeys.of(List.of(point)), AnalysisProjectKeys.of(List.of(polygon(SQUARE))));
  }

  @Test
  void no_usable_geometry_gives_no_key() {
    assertTrue(AnalysisProjectKeys.of(null).isEmpty());
    assertTrue(AnalysisProjectKeys.of(List.of()).isEmpty());
    assertTrue(AnalysisProjectKeys.of(List.of(Feature.builder().build())).isEmpty());
    assertTrue(AnalysisProjectKeys.of(List.of(feature("not a json"))).isEmpty());
  }

  @Test
  void individual_means_a_single_geometry() {
    var other = "[[[2.01,48.0],[2.011,48.0],[2.011,48.001],[2.01,48.001],[2.01,48.0]]]";

    assertTrue(AnalysisProjectKeys.isIndividual(List.of(polygon(SQUARE))));
    assertFalse(AnalysisProjectKeys.isIndividual(List.of(polygon(SQUARE), polygon(other))));
    assertFalse(AnalysisProjectKeys.isIndividual(List.of()));
    assertFalse(AnalysisProjectKeys.isIndividual(null));
    assertFalse(AnalysisProjectKeys.isIndividual(List.of(Feature.builder().build())));
  }

  @Test
  void geometry_presence_does_not_depend_on_the_number_of_features() {
    assertTrue(AnalysisProjectKeys.hasGeometry(List.of(polygon(SQUARE), polygon(SQUARE))));
    assertFalse(AnalysisProjectKeys.hasGeometry(List.of(Feature.builder().build())));
    assertFalse(AnalysisProjectKeys.hasGeometry(null));
  }

  @Test
  void multi_polygon_feature_stays_individual() {
    var multiPolygon =
        feature("{\"type\":\"MultiPolygon\",\"coordinates\":[" + SQUARE + "," + SQUARE + "]}");

    assertTrue(AnalysisProjectKeys.isIndividual(List.of(multiPolygon)));
  }

  @Test
  void no_individual_key_for_several_geometries() {
    var other = "[[[2.01,48.0],[2.011,48.0],[2.011,48.001],[2.01,48.001],[2.01,48.0]]]";

    assertTrue(
        AnalysisProjectKeys.ofIndividual(List.of(polygon(SQUARE), polygon(other))).isEmpty());
    assertEquals(
        AnalysisProjectKeys.of(List.of(polygon(SQUARE))),
        AnalysisProjectKeys.ofIndividual(List.of(polygon(SQUARE))));
  }

  @Test
  void zone_hash_tolerates_a_shift_of_a_few_decimeters_that_the_project_key_does_not() {
    var shifted =
        "[[[2.000004,48.000004],[2.001004,48.000004],[2.001004,48.001004],[2.000004,48.001004],"
            + "[2.000004,48.000004]]]";

    assertEquals(
        AnalysisProjectKeys.zoneHashOfIndividual(List.of(polygon(SQUARE))),
        AnalysisProjectKeys.zoneHashOfIndividual(List.of(polygon(shifted))));
    assertNotEquals(
        AnalysisProjectKeys.of(List.of(polygon(SQUARE))),
        AnalysisProjectKeys.of(List.of(polygon(shifted))));
  }

  @Test
  void zone_hash_distinguishes_zones_shifted_by_several_meters() {
    var shifted =
        "[[[2.0001,48.0001],[2.0011,48.0001],[2.0011,48.0011],[2.0001,48.0011],[2.0001,48.0001]]]";

    assertNotEquals(
        AnalysisProjectKeys.zoneHashOfIndividual(List.of(polygon(SQUARE))),
        AnalysisProjectKeys.zoneHashOfIndividual(List.of(polygon(shifted))));
  }

  @Test
  void zone_hash_is_prefixed_and_distinct_from_the_project_key() {
    var zoneHash = AnalysisProjectKeys.zoneHashOfIndividual(List.of(polygon(SQUARE))).orElseThrow();

    assertTrue(zoneHash.startsWith("zone-v1:"));
    assertNotEquals(AnalysisProjectKeys.of(List.of(polygon(SQUARE))).orElseThrow(), zoneHash);
  }

  @Test
  void no_zone_hash_for_several_geometries_or_no_geometry() {
    var other = "[[[2.01,48.0],[2.011,48.0],[2.011,48.001],[2.01,48.001],[2.01,48.0]]]";

    assertTrue(
        AnalysisProjectKeys.zoneHashOfIndividual(List.of(polygon(SQUARE), polygon(other)))
            .isEmpty());
    assertTrue(AnalysisProjectKeys.zoneHashOfIndividual(List.of()).isEmpty());
    assertTrue(AnalysisProjectKeys.zoneHashOfIndividual(null).isEmpty());
  }

  private static Feature polygon(String coordinates) {
    return feature("{\"type\":\"Polygon\",\"coordinates\":" + coordinates + "}");
  }

  private static Feature feature(String geometryJson) {
    return Feature.builder()
        .geometry(Feature.FeatureGeometry.builder().actualInstanceStringValue(geometryJson).build())
        .build();
  }
}
