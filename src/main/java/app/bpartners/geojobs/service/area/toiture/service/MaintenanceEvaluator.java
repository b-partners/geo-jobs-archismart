package app.bpartners.geojobs.service.area.toiture.service;

import app.bpartners.geojobs.service.area.toiture.model.MaintenancePriority;
import app.bpartners.geojobs.service.area.toiture.model.RoofContext;
import app.bpartners.geojobs.service.area.toiture.model.RoofType;
import app.bpartners.geojobs.service.area.toiture.model.VegetationContext;
import org.springframework.stereotype.Component;

@Component
public class MaintenanceEvaluator {

  /**
   * Determines autumn maintenance priority based on vegetation proximity and roof characteristics.
   *
   * <p><b>Sensitivity categories:</b>
   *
   * <ul>
   *   <li>S2 (High): distMin <= 5m AND (hasDrainageSystem OR FLAT_OR_VERY_LOW_SLOPE)
   *   <li>S0 (Low): distMin > 15m AND surfVeg30m < 200m² AND !hasDrainageSystem
   *   <li>S1 (Medium): All other cases
   * </ul>
   *
   * <b>Priority output:</b>
   *
   * <ul>
   *   <li>S2 → PRIORITAIRE
   *   <li>S1 → RECOMMANDE
   *   <li>S0 → RECOMMANDE if roof is FLAT_OR_VERY_LOW_SLOPE, otherwise NON_PRIORITAIRE
   * </ul>
   */
  public MaintenancePriority evaluate(VegetationContext vegContext, RoofContext roofContext) {
    int sensitivity = classifySensitivity(vegContext, roofContext);
    return mapToPriority(sensitivity, roofContext.roofType());
  }

  private static int classifySensitivity(VegetationContext veg, RoofContext roof) {
    // S2 check first (highest priority)
    if (veg.distMinMeters() <= 5.0
        && (roof.hasDrainageSystem() || roof.roofType() == RoofType.FLAT_OR_VERY_LOW_SLOPE)) {
      return 2; // S2
    }

    // S0 check
    if (veg.distMinMeters() > 15.0
        && veg.surfVeg30mSqMeters() < 200.0
        && !roof.hasDrainageSystem()) {
      return 0; // S0
    }

    // S1 (default)
    return 1;
  }

  private static MaintenancePriority mapToPriority(int sensitivity, RoofType roofType) {
    return switch (sensitivity) {
      case 2 -> MaintenancePriority.PRIORITAIRE;
      case 1 -> MaintenancePriority.RECOMMANDE;
      case 0 -> {
        if (roofType == RoofType.FLAT_OR_VERY_LOW_SLOPE) {
          yield MaintenancePriority.RECOMMANDE;
        }
        yield MaintenancePriority.NON_PRIORITAIRE;
      }
      default -> throw new IllegalArgumentException("Unknown sensitivity class: " + sensitivity);
    };
  }
}
