package app.bpartners.geojobs.service.area.toiture;

import static app.bpartners.geojobs.repository.model.detection.DetectableType.ARBRE;
import static app.bpartners.geojobs.repository.model.detection.DetectableType.ESPACE_VERT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import app.bpartners.geojobs.model.exception.BadRequestException;
import app.bpartners.geojobs.model.geometry.MultiPolygonObjectType;
import app.bpartners.geojobs.service.area.toiture.model.CoveringType;
import app.bpartners.geojobs.service.area.toiture.model.FireRiskLevel;
import app.bpartners.geojobs.service.area.toiture.model.MaintenancePriority;
import app.bpartners.geojobs.service.area.toiture.model.RoofContext;
import app.bpartners.geojobs.service.area.toiture.model.RoofType;
import app.bpartners.geojobs.service.area.toiture.model.VegetationIndex;
import app.bpartners.geojobs.service.area.toiture.service.FireRiskEvaluator;
import app.bpartners.geojobs.service.area.toiture.service.MaintenanceEvaluator;
import app.bpartners.geojobs.service.area.toiture.service.RoofAssessmentFacade;
import app.bpartners.geojobs.service.area.toiture.service.RoofVegetationContextEvaluator;
import app.bpartners.geojobs.service.area.toiture.service.VegetationProfiler;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.Polygon;

class RoofVegetationContextEvaluatorTest {
  private static final double LAT_METERS_PER_DEGREE = 111_320.0;
  private static final double BASE_LON = 1.52;
  private static final double BASE_LAT = 43.53;
  private static final GeometryFactory GF = new GeometryFactory();

  private static MultiPolygon rectangle(
      double lon, double lat, double widthMeters, double heightMeters) {
    var dLon = widthMeters / (LAT_METERS_PER_DEGREE * Math.cos(Math.toRadians(lat)));
    var dLat = heightMeters / LAT_METERS_PER_DEGREE;
    var coords =
        new Coordinate[] {
          new Coordinate(lon, lat),
          new Coordinate(lon + dLon, lat),
          new Coordinate(lon + dLon, lat + dLat),
          new Coordinate(lon, lat + dLat),
          new Coordinate(lon, lat),
        };
    return GF.createMultiPolygon(new Polygon[] {GF.createPolygon(coords)});
  }

  private static RoofVegetationContextEvaluator evaluatorWith(
      double roofAreaInM2, List<MultiPolygonObjectType> objectTypes) {
    var roof = rectangle(BASE_LON, BASE_LAT, 10, 10);
    return new RoofVegetationContextEvaluator(roof, roofAreaInM2, objectTypes, null, null, false);
  }

  @Test
  void compute_distance_in_meters_when_vegetation_is_adjacent() {
    // 10m x 10m roof at (1.52, 43.53), vegetation 10m wide placed 5m east of the roof
    var roof = rectangle(BASE_LON, BASE_LAT, 10, 10);
    var dLon10m = 10 / (LAT_METERS_PER_DEGREE * Math.cos(Math.toRadians(BASE_LAT)));
    var dLon5m = 5 / (LAT_METERS_PER_DEGREE * Math.cos(Math.toRadians(BASE_LAT)));
    var vegetation = rectangle(BASE_LON + dLon10m + dLon5m, BASE_LAT, 10, 10);

    var evaluator =
        new RoofVegetationContextEvaluator(
            roof, 100, List.of(new MultiPolygonObjectType(vegetation, ARBRE)), null, null, false);

    var context = evaluator.getVegetationContext();

    // The gap between roof and vegetation is 5 meters (all in Lambert-93 projection)
    assertEquals(5.0, context.distMinMeters(), 1.0);
  }

  @Test
  void compute_zero_distance_when_vegetation_overlaps_roof() {
    var roof = rectangle(BASE_LON, BASE_LAT, 10, 10);
    // 20m x 20m vegetation centered on the roof overlaps it entirely
    var vegetation = rectangle(BASE_LON, BASE_LAT, 20, 20);

    var evaluator =
        new RoofVegetationContextEvaluator(
            roof, 100, List.of(new MultiPolygonObjectType(vegetation, ARBRE)), null, null, false);

    var context = evaluator.getVegetationContext();

    assertEquals(0.0, context.distMinMeters(), 0.5);
  }

  @Test
  void count_vegetation_surface_within_30_meters_in_square_meters() {
    var roof = rectangle(BASE_LON, BASE_LAT, 10, 10);
    // 20m x 20m vegetation (400 m²) fully within 30m of the roof
    var vegetation = rectangle(BASE_LON, BASE_LAT, 20, 20);

    var evaluator =
        new RoofVegetationContextEvaluator(
            roof,
            100,
            List.of(new MultiPolygonObjectType(vegetation, ESPACE_VERT)),
            null,
            null,
            false);

    var context = evaluator.getVegetationContext();

    assertEquals(400.0, context.surfVeg30mSqMeters(), 10.0);
  }

  @Test
  void exclude_vegetation_beyond_30_meters_from_surface() {
    var roof = rectangle(BASE_LON, BASE_LAT, 10, 10);
    var dLon10m = 10 / (LAT_METERS_PER_DEGREE * Math.cos(Math.toRadians(BASE_LAT)));
    var dLon40m = 40 / (LAT_METERS_PER_DEGREE * Math.cos(Math.toRadians(BASE_LAT)));
    // vegetation placed 40m east of the roof
    var vegetation = rectangle(BASE_LON + dLon10m + dLon40m, BASE_LAT, 10, 10);

    var evaluator =
        new RoofVegetationContextEvaluator(
            roof, 100, List.of(new MultiPolygonObjectType(vegetation, ARBRE)), null, null, false);

    var context = evaluator.getVegetationContext();

    assertTrue(context.distMinMeters() > 30.0);
    assertEquals(0.0, context.surfVeg30mSqMeters(), 1.0);
  }

  @Test
  void return_no_vegetation_context_when_no_vegetation_detected() {
    var evaluator = evaluatorWith(100, List.of());

    var context = evaluator.getVegetationContext();

    // No vegetation → distance is far (> 15m, D3 class) and no surface within 30m
    assertTrue(context.distMinMeters() > 15.0);
    assertEquals(0.0, context.surfVeg30mSqMeters(), 0.0);
  }

  @Test
  void compute_area_in_square_meters_for_detectable_type() {
    var vegetation = rectangle(BASE_LON, BASE_LAT, 20, 20); // 400 m²
    var evaluator =
        evaluatorWith(100, List.of(new MultiPolygonObjectType(vegetation, ESPACE_VERT)));

    var area = evaluator.computeArea(ESPACE_VERT);

    assertEquals(400.0, area, 10.0);
  }

  @Test
  void throw_when_computing_area_with_non_positive_roof_area() {
    var evaluator = evaluatorWith(0, List.of());

    assertThrows(BadRequestException.class, () -> evaluator.computeArea(ARBRE));
  }

  @Test
  void classify_roof_type_from_slope() {
    assertEquals(
        RoofType.FLAT_OR_VERY_LOW_SLOPE,
        new RoofVegetationContextEvaluator(
                rectangle(BASE_LON, BASE_LAT, 10, 10), 100, List.of(), 2.0, null, false)
            .getRoofContext()
            .roofType());
    assertEquals(
        RoofType.LOW_SLOPE,
        new RoofVegetationContextEvaluator(
                rectangle(BASE_LON, BASE_LAT, 10, 10), 100, List.of(), 10.0, null, false)
            .getRoofContext()
            .roofType());
    assertEquals(
        RoofType.MEDIUM_SLOPE,
        new RoofVegetationContextEvaluator(
                rectangle(BASE_LON, BASE_LAT, 10, 10), 100, List.of(), 30.0, null, false)
            .getRoofContext()
            .roofType());
    assertEquals(
        RoofType.STEEP_SLOPE,
        new RoofVegetationContextEvaluator(
                rectangle(BASE_LON, BASE_LAT, 10, 10), 100, List.of(), 60.0, null, false)
            .getRoofContext()
            .roofType());
    // Unknown slope defaults to MEDIUM_SLOPE (neutral)
    assertEquals(RoofType.MEDIUM_SLOPE, evaluatorWith(100, List.of()).getRoofContext().roofType());
  }

  @Test
  void build_roof_context_with_covering_and_drainage() {
    var evaluator =
        new RoofVegetationContextEvaluator(
            rectangle(BASE_LON, BASE_LAT, 10, 10),
            100,
            List.of(),
            20.0,
            CoveringType.HIGH_COMBUSTIBILITY,
            true);

    var roofContext = evaluator.getRoofContext();

    assertEquals(
        new RoofContext(RoofType.MEDIUM_SLOPE, CoveringType.HIGH_COMBUSTIBILITY, true),
        roofContext);
  }

  @Test
  void assess_roof_end_to_end_with_vegetation_evaluator() {
    var roof = rectangle(BASE_LON, BASE_LAT, 10, 10);
    var dLon10m = 10 / (LAT_METERS_PER_DEGREE * Math.cos(Math.toRadians(BASE_LAT)));
    var dLon1m = 1 / (LAT_METERS_PER_DEGREE * Math.cos(Math.toRadians(BASE_LAT)));
    // 20m x 20m vegetation (400 m²) placed 1m east of the roof → D0 + V2 → ELEVE
    var vegetation = rectangle(BASE_LON + dLon10m + dLon1m, BASE_LAT, 20, 20);

    var facade =
        new RoofAssessmentFacade(
            new VegetationProfiler(), new FireRiskEvaluator(), new MaintenanceEvaluator());
    var evaluator =
        new RoofVegetationContextEvaluator(
            roof,
            100,
            List.of(new MultiPolygonObjectType(vegetation, ESPACE_VERT)),
            20.0,
            CoveringType.LOW_COMBUSTIBILITY,
            true);

    var result = facade.computeAssessment(evaluator);

    // distMin ≈ 1m (D0) and surfVeg30m ≈ 400 m² (V2) → ELEVE
    assertEquals(VegetationIndex.ELEVE, result.vegetationIndex());
    // MEDIUM_SLOPE neutral, LOW_COMBUSTIBILITY keeps ELEVE
    assertEquals(FireRiskLevel.ELEVE, result.fireRiskLevel());
    // distMin ≤ 5 AND hasDrainageSystem → S2 → PRIORITAIRE
    assertEquals(MaintenancePriority.PRIORITAIRE, result.maintenancePriority());
  }
}
