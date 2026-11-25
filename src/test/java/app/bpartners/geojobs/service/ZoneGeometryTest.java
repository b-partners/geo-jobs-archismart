package app.bpartners.geojobs.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import app.bpartners.geojobs.repository.model.Feature;
import java.util.List;
import org.junit.jupiter.api.Test;

class ZoneGeometryTest {
  static final String SQUARE = "[[[2.0,48.0],[2.001,48.0],[2.001,48.001],[2.0,48.001],[2.0,48.0]]]";
  static final String SHIFTED_40_CM =
      "[[[2.000004,48.000004],[2.001004,48.000004],[2.001004,48.001004],[2.000004,48.001004],"
          + "[2.000004,48.000004]]]";
  static final String DISJOINT =
      "[[[2.01,48.0],[2.011,48.0],[2.011,48.001],[2.01,48.001],[2.01,48.0]]]";

  @Test
  void identical_zones_overlap_completely_without_displacement() {
    var zone = zone(polygon(SQUARE));

    assertEquals(1.0, zone.iouWith(zone), 1e-9);
    assertTrue(zone.isWithinDisplacementOf(zone, 0.1));
  }

  @Test
  void a_zone_shifted_by_a_few_decimeters_still_overlaps_almost_completely() {
    var zone = zone(polygon(SQUARE));
    var shifted = zone(polygon(SHIFTED_40_CM));

    assertTrue(zone.iouWith(shifted) > 0.98);
    assertTrue(zone.isWithinDisplacementOf(shifted, 1.0));
    assertTrue(shifted.isWithinDisplacementOf(zone, 1.0));
    assertFalse(zone.isWithinDisplacementOf(shifted, 0.2));
    assertEquals(zone.iouWith(shifted), shifted.iouWith(zone), 1e-9);
  }

  @Test
  void disjoint_zones_do_not_overlap() {
    var zone = zone(polygon(SQUARE));
    var disjoint = zone(polygon(DISJOINT));

    assertEquals(0.0, zone.iouWith(disjoint), 1e-9);
    assertFalse(zone.isWithinDisplacementOf(disjoint, 1.0));
  }

  @Test
  void bounds_enclose_the_zone() {
    var bounds = zone(polygon(SQUARE)).bounds();

    assertEquals(new ZoneBounds(2.0, 48.0, 2.001, 48.001), bounds);
  }

  @Test
  void only_an_individual_polygon_has_a_geometry() {
    assertTrue(ZoneGeometry.ofIndividual(List.of(polygon(SQUARE), polygon(DISJOINT))).isEmpty());
    assertTrue(ZoneGeometry.ofIndividual(List.of()).isEmpty());
    assertTrue(ZoneGeometry.ofIndividual(null).isEmpty());
    assertTrue(
        ZoneGeometry.ofIndividual(
                List.of(geometry("{\"type\":\"Point\",\"coordinates\":[2.0,48.0]}")))
            .isEmpty());
  }

  @Test
  void a_multipolygon_is_a_geometry() {
    var multiPolygon = geometry("{\"type\":\"MultiPolygon\",\"coordinates\":[" + SQUARE + "]}");

    assertEquals(1.0, zone(multiPolygon).iouWith(zone(polygon(SQUARE))), 1e-9);
  }

  private static ZoneGeometry zone(Feature feature) {
    return ZoneGeometry.ofIndividual(List.of(feature)).orElseThrow();
  }

  private static Feature polygon(String coordinates) {
    return geometry("{\"type\":\"Polygon\",\"coordinates\":" + coordinates + "}");
  }

  private static Feature geometry(String json) {
    return Feature.builder()
        .geometry(Feature.FeatureGeometry.builder().actualInstanceStringValue(json).build())
        .build();
  }
}
