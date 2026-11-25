package app.bpartners.geojobs.service;

import static java.lang.Math.PI;
import static java.lang.Math.cos;
import static java.lang.Math.sin;
import static java.lang.Math.toRadians;
import static org.junit.jupiter.api.Assertions.assertTrue;

import app.bpartners.geojobs.repository.model.Feature;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.Test;

class ZoneGeometryPerformanceTest {
  static final int WARM_UP_RUNS = 5;
  static final int MEASURED_RUNS = 9;
  static final double RADIUS_IN_METERS = 30.0;
  static final double SHIFT_IN_METERS = 0.4;
  static final double CENTER_LON = 2.0;
  static final double CENTER_LAT = 48.0;
  static final double METERS_PER_DEGREE = ZoneGeometry.METERS_PER_DEGREE;

  @Test
  void parsing_and_comparing_zones_stays_within_budget_for_every_polygon_size() {
    var report =
        new StringBuilder("\nvertices | parse(ms) | iou(ms) | displacement(ms) | total(ms)\n");
    for (var vertices : List.of(10, 100, ZoneGeometry.MAX_COORDINATES - 1)) {
      var target = polygon(vertices, 0.0);
      var source = polygon(vertices, SHIFT_IN_METERS);

      var parse = medianMillis(() -> parse(target, source));
      var targetZone = zone(target);
      var sourceZone = zone(source);
      var iou = medianMillis(() -> targetZone.iouWith(sourceZone));
      var displacement = medianMillis(() -> targetZone.isWithinDisplacementOf(sourceZone, 1.0));
      var total = parse + iou + displacement;

      report.append(
          "%8d | %9.2f | %7.2f | %16.2f | %9.2f%n"
              .formatted(vertices, parse, iou, displacement, total));
      assertTrue(
          total <= budgetInMillis(vertices),
          "comparing two zones of " + vertices + " vertices took " + total + " ms" + report);
    }
    System.out.println(report);
  }

  @Test
  void checking_twenty_nearby_candidates_stays_within_budget_for_every_polygon_size() {
    var report = new StringBuilder("\nvertices | 20 candidates(ms)\n");
    for (var vertices : List.of(10, 100, ZoneGeometry.MAX_COORDINATES - 1)) {
      var target = zone(polygon(vertices, 0.0));
      var candidates =
          java.util.stream.IntStream.range(0, 20)
              .mapToObj(index -> polygon(vertices, SHIFT_IN_METERS + index * 0.1))
              .toList();

      var elapsed =
          medianMillis(
              () -> {
                for (var candidate : candidates) {
                  var candidateZone = zone(candidate);
                  target.iouWith(candidateZone);
                  target.isWithinDisplacementOf(candidateZone, 1.0);
                }
              });

      report.append("%8d | %18.2f%n".formatted(vertices, elapsed));
      assertTrue(
          elapsed <= 20 * budgetInMillis(vertices) / 2,
          "checking 20 candidates of " + vertices + " vertices took " + elapsed + " ms" + report);
    }
    System.out.println(report);
  }

  @Test
  void zones_above_the_vertex_limit_are_rejected_quickly_without_being_compared() {
    var oversized = polygon(5_000, 0.0);

    var elapsed =
        medianMillis(() -> assertTrue(ZoneGeometry.ofIndividual(List.of(oversized)).isEmpty()));

    assertTrue(elapsed <= 200, "rejecting an oversized zone took " + elapsed + " ms");
  }

  private static double budgetInMillis(int vertices) {
    return switch (vertices) {
      case 10, 100 -> 50;
      default -> 400;
    };
  }

  private static void parse(Feature target, Feature source) {
    zone(target);
    zone(source);
  }

  private static ZoneGeometry zone(Feature feature) {
    return ZoneGeometry.ofIndividual(List.of(feature)).orElseThrow();
  }

  private static double medianMillis(Runnable action) {
    for (int i = 0; i < WARM_UP_RUNS; i++) {
      action.run();
    }
    var samples = new ArrayList<Double>();
    for (int i = 0; i < MEASURED_RUNS; i++) {
      var start = System.nanoTime();
      action.run();
      samples.add((System.nanoTime() - start) / 1_000_000.0);
    }
    samples.sort(Double::compare);
    return samples.get(samples.size() / 2);
  }

  private static Feature polygon(int vertices, double eastShiftInMeters) {
    var random = new Random(42);
    var cosLat = cos(toRadians(CENTER_LAT));
    var ring = new StringBuilder();
    double firstLon = 0;
    double firstLat = 0;
    for (int i = 0; i < vertices; i++) {
      var angle = 2 * PI * i / vertices;
      var radius = RADIUS_IN_METERS * (0.95 + 0.1 * random.nextDouble());
      var lon =
          CENTER_LON + (radius * cos(angle) + eastShiftInMeters) / (METERS_PER_DEGREE * cosLat);
      var lat = CENTER_LAT + radius * sin(angle) / METERS_PER_DEGREE;
      if (i == 0) {
        firstLon = lon;
        firstLat = lat;
      }
      ring.append("[").append(lon).append(",").append(lat).append("],");
    }
    ring.append("[").append(firstLon).append(",").append(firstLat).append("]");
    var json = "{\"type\":\"Polygon\",\"coordinates\":[[" + ring + "]]}";
    return Feature.builder()
        .geometry(Feature.FeatureGeometry.builder().actualInstanceStringValue(json).build())
        .build();
  }
}
