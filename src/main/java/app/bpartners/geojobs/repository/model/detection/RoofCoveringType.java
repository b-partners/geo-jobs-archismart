package app.bpartners.geojobs.repository.model.detection;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.util.Arrays;

/**
 * kUsure/kMoisissure/kHumidite sont les coefficients de sensibilite du revetement utilises par le
 * score de degradation (methodologie V2.2) : neutres (1.00) quand le revetement est inconnu.
 */
public enum RoofCoveringType {
  ROOF_ARDOISE(1.05, 0.95, 1.00),
  ROOF_ASPHALTE_BITUME(1.05, 0.90, 1.15),
  ROOF_BAC_ACIER(0.95, 0.70, 1.00),
  ROOF_BETON_BRUT(1.00, 0.85, 1.10),
  ROOF_FIBRO_CIMENT(1.10, 0.90, 1.05),
  ROOF_GRAVIER(0.95, 0.85, 1.10),
  ROOF_MEMBRANE_SYNTHETIQUE(1.00, 0.75, 1.15),
  ROOF_TOLE_ONDULEE(1.00, 0.75, 1.00),
  ROOF_TUILES(1.00, 1.00, 1.00),
  ROOF_ZINC(0.95, 0.70, 0.95);

  public final double kUsure;
  public final double kMoisissure;
  public final double kHumidite;

  RoofCoveringType(double kUsure, double kMoisissure, double kHumidite) {
    this.kUsure = kUsure;
    this.kMoisissure = kMoisissure;
    this.kHumidite = kHumidite;
  }

  public static RoofCoveringType fromModelLabel(String label) {
    var normalized = label == null ? "" : label.toUpperCase();
    if (normalized.equals("FIBRE_CIMENT")) {
      return ROOF_FIBRO_CIMENT;
    }
    return fromString(normalized.startsWith("ROOF_") ? normalized : "ROOF_" + normalized);
  }

  // Used for mapping
  @JsonCreator
  public static RoofCoveringType fromString(String value) {
    return Arrays.stream(values())
        .filter(e -> e.name().equalsIgnoreCase(value))
        .findFirst()
        .orElseThrow(() -> new IllegalArgumentException("Unknown value: " + value));
  }
}
