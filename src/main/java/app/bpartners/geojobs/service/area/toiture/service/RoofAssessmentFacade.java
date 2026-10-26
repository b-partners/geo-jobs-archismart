package app.bpartners.geojobs.service.area.toiture.service;

import app.bpartners.geojobs.service.area.toiture.model.RoofAssessmentResult;
import app.bpartners.geojobs.service.area.toiture.model.RoofContext;
import app.bpartners.geojobs.service.area.toiture.model.VegetationContext;
import app.bpartners.geojobs.service.area.toiture.model.VegetationIndex;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RoofAssessmentFacade {

  private final VegetationProfiler vegetationProfiler;
  private final FireRiskEvaluator fireRiskEvaluator;
  private final MaintenanceEvaluator maintenanceEvaluator;

  public RoofAssessmentResult computeAssessment(
      RoofVegetationContextEvaluator roofVegetationContextEvaluator) {
    return computeAssessment(
        roofVegetationContextEvaluator.getVegetationContext(),
        roofVegetationContextEvaluator.getRoofContext());
  }

  public RoofAssessmentResult computeAssessment(VegetationContext veg, RoofContext roof) {
    VegetationIndex vegIndex = vegetationProfiler.evaluate(veg);

    var fireRisk = fireRiskEvaluator.evaluate(vegIndex, roof);
    var maintenance = maintenanceEvaluator.evaluate(veg, roof);

    return new RoofAssessmentResult(vegIndex, fireRisk, maintenance);
  }
}
