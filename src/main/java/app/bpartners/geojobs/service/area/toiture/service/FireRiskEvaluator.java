package app.bpartners.geojobs.service.area.toiture.service;

import app.bpartners.geojobs.service.area.toiture.model.CoveringType;
import app.bpartners.geojobs.service.area.toiture.model.FireRiskLevel;
import app.bpartners.geojobs.service.area.toiture.model.RoofContext;
import app.bpartners.geojobs.service.area.toiture.model.RoofType;
import app.bpartners.geojobs.service.area.toiture.model.VegetationIndex;
import org.springframework.stereotype.Component;

@Component
public class FireRiskEvaluator {

  /**
   * Calculates fire risk by shifting VegetationIndex based on roof slope and covering type.
   *
   * <ul>
   *   <li>FLAT_OR_VERY_LOW_SLOPE → step UP risk (+1 level)
   *   <li>STEEP_SLOPE → step DOWN risk (-1 level)
   *   <li>MEDIUM_SLOPE / LOW_SLOPE → neutral
   *   <li>HIGH_COMBUSTIBILITY → minimum MODERE if vegetation exists (vegIndex != NULL)
   *   <li>LOW_COMBUSTIBILITY → if result is FAIBLE, allow reduction to NULL
   * </ul>
   */
  public FireRiskLevel evaluate(VegetationIndex vegIndex, RoofContext roof) {
    // 1. Apply slope modifier
    VegetationIndex afterSlope = applySlopeModifier(vegIndex, roof.roofType());

    // 2. Apply covering-type modifier
    return applyCoveringModifier(afterSlope, roof.coveringType());
  }

  private static VegetationIndex applySlopeModifier(VegetationIndex index, RoofType roofType) {
    return switch (roofType) {
      case FLAT_OR_VERY_LOW_SLOPE -> index.stepUp();
      case STEEP_SLOPE -> index.stepDown();
      case LOW_SLOPE, MEDIUM_SLOPE -> index;
    };
  }

  private static FireRiskLevel applyCoveringModifier(
      VegetationIndex afterSlope, CoveringType coveringType) {
    return switch (coveringType) {
      case HIGH_COMBUSTIBILITY -> {
        // Ensure minimum MODERE if vegetation exists
        if (afterSlope != VegetationIndex.NULL) {
          yield toFireRiskLevel(enforceMinimumModere(afterSlope));
        }
        yield toFireRiskLevel(afterSlope);
      }
      case LOW_COMBUSTIBILITY -> {
        // If result is FAIBLE, allow reduction to NULL
        if (afterSlope == VegetationIndex.FAIBLE) {
          yield FireRiskLevel.NULL;
        }
        yield toFireRiskLevel(afterSlope);
      }
    };
  }

  private static VegetationIndex enforceMinimumModere(VegetationIndex index) {
    return switch (index) {
      case NULL, FAIBLE -> VegetationIndex.MODERE;
      case MODERE, ELEVE -> index;
    };
  }

  private static FireRiskLevel toFireRiskLevel(VegetationIndex index) {
    return switch (index) {
      case NULL -> FireRiskLevel.NULL;
      case FAIBLE -> FireRiskLevel.FAIBLE;
      case MODERE -> FireRiskLevel.MODERE;
      case ELEVE -> FireRiskLevel.ELEVE;
    };
  }
}
