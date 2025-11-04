package app.bpartners.geojobs.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import app.bpartners.geojobs.repository.model.detection.DetectableType;
import app.bpartners.geojobs.service.annotator.LabelConverter;
import org.junit.jupiter.api.Test;

class LabelConverterTest {

  LabelConverter subject = new LabelConverter();

  @Test
  void get_label_color() {
    var arbre = subject.apply(DetectableType.ARBRE);
    var toitureRevetement = subject.apply(DetectableType.TOITURE_REVETEMENT);
    var panneau = subject.apply(DetectableType.PANNEAU_PHOTOVOLTAIQUE);
    var piscine = subject.apply(DetectableType.PISCINE);
    var passagePieton = subject.apply(DetectableType.PASSAGE_PIETON);
    var trottoir = subject.apply(DetectableType.TROTTOIR);
    var ligne = subject.apply(DetectableType.LINE);
    var espaceVert = subject.apply(DetectableType.ESPACE_VERT);
    var voieCarrosable = subject.apply(DetectableType.VOIE_CARROSSABLE);
    var parking = subject.apply(DetectableType.PARKING);
    var moisissure = subject.apply(DetectableType.MOISISSURE_CLAIR);
    var usure = subject.apply(DetectableType.USURE_IMPORTANTE);
    var fissureCassure = subject.apply(DetectableType.FISSURE_CASSURE);
    var obstacle = subject.apply(DetectableType.OBSTACLE);
    var cheminee = subject.apply(DetectableType.CHEMINEE);
    var humidite = subject.apply(DetectableType.HUMIDITE_INTENSE);
    var risqueFeu = subject.apply(DetectableType.RISQUE_FEU);
    var velux = subject.apply(DetectableType.VELUX);
    var batiTuiles = subject.apply(DetectableType.BATI_TUILES);
    var batiBeton = subject.apply(DetectableType.BATI_BETON);
    var batiArdoise = subject.apply(DetectableType.BATI_ARDOISE);
    var batiAutres = subject.apply(DetectableType.BATI_AUTRES);
    var espaceVertParking = subject.apply(DetectableType.ESPACE_VERT_PARKING);

    assertEquals("#4BFF33", arbre.getColor());
    assertEquals("#DFFF00", toitureRevetement.getColor());
    assertEquals("#0E4EB3", panneau.getColor());
    assertEquals("#0DCBD2", piscine.getColor());
    assertEquals("#F5F586", passagePieton.getColor());
    assertEquals("#54deb7", trottoir.getColor());
    assertEquals("#ff3388", ligne.getColor());
    assertEquals("#e39724", espaceVert.getColor());
    assertEquals("TODO", voieCarrosable.getColor());
    assertEquals("#8c463e", parking.getColor());
    assertEquals("#5d8c3e", moisissure.getColor());
    assertEquals("#3e718c", usure.getColor());
    assertEquals("#733e8c", fissureCassure.getColor());
    assertEquals("#3e8c88", obstacle.getColor());
    assertEquals("#a32a55", cheminee.getColor());
    assertEquals("#f2f538", humidite.getColor());
    assertEquals("#361c1b", risqueFeu.getColor());
    assertEquals("#c71497", velux.getColor());
    assertEquals("#47e66c", batiTuiles.getColor());
    assertEquals("#425c20", batiBeton.getColor());
    assertEquals("#5299bf", batiArdoise.getColor());
    assertEquals("#de6ce0", batiAutres.getColor());
    assertEquals("#93c47d", espaceVertParking.getColor());
  }
}
