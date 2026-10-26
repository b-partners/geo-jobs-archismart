package app.bpartners.geojobs.service.area.toiture.model;

public enum VegetationIndex {
  NULL,
  FAIBLE,
  MODERE,
  ELEVE;

  public VegetationIndex stepUp() {
    return switch (this) {
      case NULL -> FAIBLE;
      case FAIBLE -> MODERE;
      case MODERE -> ELEVE;
      case ELEVE -> ELEVE;
    };
  }

  public VegetationIndex stepDown() {
    return switch (this) {
      case ELEVE -> MODERE;
      case MODERE -> FAIBLE;
      case FAIBLE -> NULL;
      case NULL -> NULL;
    };
  }
}
