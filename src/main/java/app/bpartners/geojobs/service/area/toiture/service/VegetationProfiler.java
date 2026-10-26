package app.bpartners.geojobs.service.area.toiture.service;

import static app.bpartners.geojobs.service.area.toiture.model.VegetationIndex.ELEVE;
import static app.bpartners.geojobs.service.area.toiture.model.VegetationIndex.FAIBLE;
import static app.bpartners.geojobs.service.area.toiture.model.VegetationIndex.MODERE;
import static app.bpartners.geojobs.service.area.toiture.model.VegetationIndex.NULL;

import app.bpartners.geojobs.service.area.toiture.model.VegetationContext;
import app.bpartners.geojobs.service.area.toiture.model.VegetationIndex;
import org.springframework.stereotype.Component;

@Component
public class VegetationProfiler {

  private static final double D0_MAX = 2.0;
  private static final double D1_MAX = 5.0;
  private static final double D2_MAX = 15.0;

  private static final double V0_MAX = 50.0;
  private static final double V1_MAX = 200.0;
  private static final double V2_MAX = 500.0;

  /**
   * Distance classes: D0 (≤ 2m), D1 (2–5m], D2 (5–15m], D3 (> 15m) Volume classes (30m): V0 (<
   * 50m²), V1 [50–200m²), V2 [200–500m²), V3 (> 500m²)
   */
  public VegetationIndex evaluate(VegetationContext context) {
    int distClass = classifyDistance(context.distMinMeters());
    int volClass = classifyVolume(context.surfVeg30mSqMeters());
    return matrixLookup(distClass, volClass);
  }

  private static int classifyDistance(double distMinMeters) {
    if (distMinMeters <= D0_MAX) return 0; // D0
    if (distMinMeters <= D1_MAX) return 1; // D1
    if (distMinMeters <= D2_MAX) return 2; // D2
    return 3; // D3
  }

  private static int classifyVolume(double surfVeg30mSqMeters) {
    if (surfVeg30mSqMeters < V0_MAX) return 0; // V0 (< 50m²)
    if (surfVeg30mSqMeters <= V1_MAX) return 1; // V1 [50–200m²]
    if (surfVeg30mSqMeters <= V2_MAX) return 2; // V2 (200–500m²]
    return 3; // V3 (> 500m²)
  }

  private static VegetationIndex matrixLookup(int distClass, int volClass) {
    return switch (distClass) {
      case 0 -> // D0
          switch (volClass) {
            case 0 -> MODERE;
            case 1, 2, 3 -> ELEVE;
            default -> throw new IllegalArgumentException("Unknown volume class: " + volClass);
          };
      case 1 -> // D1
          switch (volClass) {
            case 0 -> FAIBLE;
            case 1, 2 -> MODERE;
            case 3 -> ELEVE;
            default -> throw new IllegalArgumentException("Unknown volume class: " + volClass);
          };
      case 2 -> // D2
          switch (volClass) {
            case 0 -> NULL;
            case 1 -> FAIBLE;
            case 2 -> MODERE;
            case 3 -> ELEVE;
            default -> throw new IllegalArgumentException("Unknown volume class: " + volClass);
          };
      case 3 -> // D3
          switch (volClass) {
            case 0, 1 -> NULL;
            case 2 -> FAIBLE;
            case 3 -> MODERE;
            default -> throw new IllegalArgumentException("Unknown volume class: " + volClass);
          };
      default -> throw new IllegalArgumentException("Unknown distance class: " + distClass);
    };
  }
}
