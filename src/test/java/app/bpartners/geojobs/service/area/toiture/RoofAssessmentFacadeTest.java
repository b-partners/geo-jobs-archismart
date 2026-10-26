package app.bpartners.geojobs.service.area.toiture;

import static org.junit.jupiter.api.Assertions.assertEquals;

import app.bpartners.geojobs.service.area.toiture.model.CoveringType;
import app.bpartners.geojobs.service.area.toiture.model.FireRiskLevel;
import app.bpartners.geojobs.service.area.toiture.model.MaintenancePriority;
import app.bpartners.geojobs.service.area.toiture.model.RoofContext;
import app.bpartners.geojobs.service.area.toiture.model.RoofType;
import app.bpartners.geojobs.service.area.toiture.model.VegetationContext;
import app.bpartners.geojobs.service.area.toiture.model.VegetationIndex;
import app.bpartners.geojobs.service.area.toiture.service.FireRiskEvaluator;
import app.bpartners.geojobs.service.area.toiture.service.MaintenanceEvaluator;
import app.bpartners.geojobs.service.area.toiture.service.RoofAssessmentFacade;
import app.bpartners.geojobs.service.area.toiture.service.VegetationProfiler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class RoofAssessmentFacadeTest {

  private RoofAssessmentFacade facade;

  @BeforeEach
  void setUp() {
    facade =
        new RoofAssessmentFacade(
            new VegetationProfiler(), new FireRiskEvaluator(), new MaintenanceEvaluator());
  }

  @Test
  void flat_roof_near_dense_vegetation_with_combustible_covering() {
    // Flat roof (0–5°) with high-combustibility covering (shingle), very close to dense vegetation
    var veg = new VegetationContext(1.5 /* meters */, 300.0 /* m² in 30m radius */);
    var roof =
        new RoofContext(
            RoofType.FLAT_OR_VERY_LOW_SLOPE,
            CoveringType.HIGH_COMBUSTIBILITY,
            true /* has drainage */);

    var result = facade.computeAssessment(veg, roof);

    // D0 (≤ 2m), V2 (200–500] → ELEVE vegetation index
    assertEquals(VegetationIndex.ELEVE, result.vegetationIndex());

    // Flat roof → step up from ELEVE → ELEVE (capped)
    // HIGH_COMBUSTIBILITY with vegetation → minimum MODERE enforced → stays ELEVE
    assertEquals(FireRiskLevel.ELEVE, result.fireRiskLevel());

    // distMin=1.5 ≤ 5 AND hasDrainageSystem=true → S2 → PRIORITAIRE
    assertEquals(MaintenancePriority.PRIORITAIRE, result.maintenancePriority());
  }

  @Test
  void steep_roof_far_from_vegetation_with_low_combustibility() {
    // Steep roof (>45°) with metal (low-combustibility), far from any significant vegetation
    var veg = new VegetationContext(20.0 /* meters */, 30.0 /* m² */);
    var roof =
        new RoofContext(
            RoofType.STEEP_SLOPE, CoveringType.LOW_COMBUSTIBILITY, false /* no drainage */);

    var result = facade.computeAssessment(veg, roof);

    // D3 (> 15m), V0 (< 50m²) → NULL vegetation index
    assertEquals(VegetationIndex.NULL, result.vegetationIndex());

    // Steep slope → step down from NULL → NULL (capped at floor)
    // LOW_COMBUSTIBILITY: FAIBLE → NULL, but we're already NULL so stays NULL
    assertEquals(FireRiskLevel.NULL, result.fireRiskLevel());

    // distMin=20 > 15 AND surfVeg30m=30 < 200 AND !hasDrainageSystem → S0
    // S0 with STEEP_SLOPE (not flat) → NON_PRIORITAIRE
    assertEquals(MaintenancePriority.NON_PRIORITAIRE, result.maintenancePriority());
  }

  @Test
  void low_slope_roof_moderate_distance_medium_vegetation_high_combustibility() {
    // Low slope roof with shingles (high combustibility), moderate distance to vegetation
    var veg = new VegetationContext(8.0 /* meters */, 120.0 /* m² */);
    var roof =
        new RoofContext(
            RoofType.LOW_SLOPE, CoveringType.HIGH_COMBUSTIBILITY, false /* no drainage */);

    var result = facade.computeAssessment(veg, roof);

    // D2 (5–15m], V1 [50–200) → FAIBLE vegetation index
    assertEquals(VegetationIndex.FAIBLE, result.vegetationIndex());

    // LOW_SLOPE → neutral (no step)
    // HIGH_COMBUSTIBILITY with vegetation (FAIBLE) → minimum MODERE enforced
    assertEquals(FireRiskLevel.MODERE, result.fireRiskLevel());

    // distMin=8 > 5 → not S2
    // distMin=8 ≤ 15 → not S0 either
    // → S1 → RECOMMANDE
    assertEquals(MaintenancePriority.RECOMMANDE, result.maintenancePriority());
  }

  @Test
  void flat_roof_far_vegetation_no_drainage_low_combustibility() {
    // Flat roof with low-combustibility membrane, far from vegetation, no drainage system
    var veg = new VegetationContext(20.0 /* meters */, 100.0 /* m² */);
    var roof =
        new RoofContext(
            RoofType.FLAT_OR_VERY_LOW_SLOPE,
            CoveringType.LOW_COMBUSTIBILITY,
            false /* no drainage */);

    var result = facade.computeAssessment(veg, roof);

    // D3 (> 15m), V1 [50–200) → NULL vegetation index
    assertEquals(VegetationIndex.NULL, result.vegetationIndex());

    // Flat roof → step up from NULL → FAIBLE
    // LOW_COMBUSTIBILITY: FAIBLE → NULL (reduction allowed)
    assertEquals(FireRiskLevel.NULL, result.fireRiskLevel());

    // distMin=20 > 15 AND surfVeg30m=100 < 200 AND !hasDrainageSystem → S0
    // S0 with FLAT_OR_VERY_LOW_SLOPE → RECOMMANDE
    assertEquals(MaintenancePriority.RECOMMANDE, result.maintenancePriority());
  }

  @Test
  void medium_slope_roof_very_close_but_sparse_vegetation() {
    // Medium slope roof, very close to vegetation but sparse (small surface area)
    var veg = new VegetationContext(1.0 /* meter */, 20.0 /* m² */);
    var roof =
        new RoofContext(
            RoofType.MEDIUM_SLOPE, CoveringType.HIGH_COMBUSTIBILITY, false /* no drainage */);

    var result = facade.computeAssessment(veg, roof);

    // D0 (≤ 2m), V0 (< 50m²) → MODERE vegetation index
    assertEquals(VegetationIndex.MODERE, result.vegetationIndex());

    // MEDIUM_SLOPE → neutral (no step)
    // HIGH_COMBUSTIBILITY with vegetation → minimum MODERE enforced → stays MODERE
    assertEquals(FireRiskLevel.MODERE, result.fireRiskLevel());

    // distMin=1 ≤ 5 AND FLAT_OR_VERY_LOW_SLOPE=false → only S2 if hasDrainageSystem
    // No drainage → not S2
    // Not S0 either (surfVeg30m=20 < 200 but distMin=1 ≤ 15)
    // → S1 → RECOMMANDE
    assertEquals(MaintenancePriority.RECOMMANDE, result.maintenancePriority());
  }

  @Test
  void moderate_distance_veg_high_combustibility_flat_roof() {
    // Moderate distances leading to a MODERE vegetation index,
    // flat roof with high combustibility
    var veg = new VegetationContext(4.0 /* meters */, 80.0 /* m² */);
    var roof =
        new RoofContext(
            RoofType.FLAT_OR_VERY_LOW_SLOPE,
            CoveringType.HIGH_COMBUSTIBILITY,
            false /* no drainage */);

    var result = facade.computeAssessment(veg, roof);

    // D1 (2–5m], V1 [50–200) → MODERE vegetation index
    assertEquals(VegetationIndex.MODERE, result.vegetationIndex());

    // Flat roof → step up from MODERE → ELEVE
    // HIGH_COMBUSTIBILITY with vegetation → minimum MODERE enforced → stays ELEVE
    assertEquals(FireRiskLevel.ELEVE, result.fireRiskLevel());

    // distMin=4 ≤ 5 AND (FLAT_OR_VERY_LOW_SLOPE=true) → S2 → PRIORITAIRE
    assertEquals(MaintenancePriority.PRIORITAIRE, result.maintenancePriority());
  }

  @Test
  void low_slope_roof_low_combustibility_high_vegetation() {
    // Low slope roof with metal covering, very close to very dense vegetation
    var veg = new VegetationContext(0.5 /* meters */, 600.0 /* m² */);
    var roof =
        new RoofContext(
            RoofType.LOW_SLOPE, CoveringType.LOW_COMBUSTIBILITY, true /* has drainage */);

    var result = facade.computeAssessment(veg, roof);

    // D0 (≤ 2m), V3 (> 500m²) → ELEVE vegetation index
    assertEquals(VegetationIndex.ELEVE, result.vegetationIndex());

    // LOW_SLOPE → neutral (no step)
    // LOW_COMBUSTIBILITY → FAIBLE → NULL would apply but we're ELEVE, stays ELEVE
    assertEquals(FireRiskLevel.ELEVE, result.fireRiskLevel());

    // distMin=0.5 ≤ 5 AND hasDrainageSystem=true → S2 → PRIORITAIRE
    assertEquals(MaintenancePriority.PRIORITAIRE, result.maintenancePriority());
  }

  @Test
  void edge_case_null_vegetation_and_no_drainage_steep_slope() {
    // No vegetation at all, steep slope, no drainage
    var veg = new VegetationContext(50.0 /* meters */, 0.0 /* m² */);
    var roof =
        new RoofContext(
            RoofType.STEEP_SLOPE, CoveringType.LOW_COMBUSTIBILITY, false /* no drainage */);

    var result = facade.computeAssessment(veg, roof);

    // D3 (> 15m), V0 (< 50m²) → NULL vegetation index
    assertEquals(VegetationIndex.NULL, result.vegetationIndex());

    // Steep slope → step down from NULL → NULL (boundary safe)
    // LOW_COMBUSTIBILITY: FAIBLE → NULL but we're NULL, stays NULL
    assertEquals(FireRiskLevel.NULL, result.fireRiskLevel());

    // distMin=50 > 15 AND surfVeg30m=0 < 200 AND !hasDrainageSystem → S0
    // S0 with STEEP_SLOPE (not flat) → NON_PRIORITAIRE
    assertEquals(MaintenancePriority.NON_PRIORITAIRE, result.maintenancePriority());
  }

  @Test
  void flat_roof_gutter_maintenance_scenario() {
    // Flat roof, moderate vegetation at medium distance, no drainage (gutters absent)
    var veg = new VegetationContext(10.0 /* meters */, 300.0 /* m² */);
    var roof =
        new RoofContext(
            RoofType.FLAT_OR_VERY_LOW_SLOPE,
            CoveringType.LOW_COMBUSTIBILITY,
            false /* no drainage */);

    var result = facade.computeAssessment(veg, roof);

    // D2 (5–15m], V2 (200–500] → MODERE vegetation index
    assertEquals(VegetationIndex.MODERE, result.vegetationIndex());

    // Flat roof → step up from MODERE → ELEVE
    // LOW_COMBUSTIBILITY: NOT FAIBLE → stays ELEVE
    assertEquals(FireRiskLevel.ELEVE, result.fireRiskLevel());

    // distMin=10 > 5 → not S2
    // distMin=10 ≤ 15 → not S0 (needs >15)
    // → S1 → RECOMMANDE
    assertEquals(MaintenancePriority.RECOMMANDE, result.maintenancePriority());
  }
}
