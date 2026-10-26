package app.bpartners.geojobs.model.geometry.area;

import static app.bpartners.geojobs.repository.model.detection.DetectableType.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

import app.bpartners.geojobs.model.DetectedTile;
import app.bpartners.geojobs.model.geometry.PolygonObjectType;
import app.bpartners.geojobs.model.geometry.area.rate.AreaRateComputerFacade;
import java.util.List;
import org.junit.jupiter.api.Test;

class AreaRateComputerFacadeTest extends AreaRateComputerTest {
  @Test
  void get_usure_area_rate_returns_expected_value() {
    var roof = createSquare(10); // Area = 100
    var usureLeger = createSquare(1); // Area = 1
    var usureImportante = createSquare(2); // Area = 4

    var polygonObjectTypes =
        List.of(
            new PolygonObjectType(usureLeger, USURE_LEGER),
            new PolygonObjectType(usureImportante, USURE_IMPORTANTE));

    var subject = new AreaRateComputerFacade(roof, polygonObjectTypes);

    // (1 * 1 + 2 * 4) / 100 * 100 = 9.0
    assertEquals(9.0, subject.getUsureAreaRate());
  }

  @Test
  void get_moisissure_area_rate_returns_expected_value() {
    var roof = createSquare(10); // Area = 100
    var mNoircie = createSquare(1); // Area = 1
    var mClair = createSquare(2); // Area = 4
    var mCouleur = createSquare(3); // Area = 9

    var polygonObjectTypes =
        List.of(
            new PolygonObjectType(mNoircie, MOISISSURE_NOIRCIE),
            new PolygonObjectType(mClair, MOISISSURE_CLAIR),
            new PolygonObjectType(mCouleur, MOISISSURE_COULEUR));

    var subject = new AreaRateComputerFacade(roof, polygonObjectTypes);

    // (1 + 4 + 9) / 100 * 100 = 14.0
    assertEquals(14.0, subject.getMoisissureAreaRate());
  }

  @Test
  void get_humidity_area_rate_returns_expected_value() {
    var roof = createSquare(10); // Area = 100
    var hClair = createSquare(1); // Area = 1
    var hIntense = createSquare(2); // Area = 4

    var polygonObjectTypes =
        List.of(
            new PolygonObjectType(hClair, HUMIDITE_CLAIR),
            new PolygonObjectType(hIntense, HUMIDITE_INTENSE));

    var subject = new AreaRateComputerFacade(roof, polygonObjectTypes);

    // (1 * 1 + 2 * 4) / 100 * 100 = 9.0
    assertEquals(9.0, subject.getHumidityAreaRate());
  }

  @Test
  void get_global_rate_returns_expected_value() {
    var roof = createSquare(10); // Area = 100

    // Usure = 1.0, Moisissure = 1.0, Humidite = 1.0 (each a single 1m2 detection on a 100m2 roof)
    // Moisissure amortie : mEff = 1 * (0.55 + 0.45 * 1/100) = 0.5545
    // Humidite renforcee : hEff = 1 (<= 20, pas de renforcement)
    // Score = 0.55 * 1 + 0.35 * 0.5545 + 0.65 * 1 = 0.55 + 0.194075 + 0.65 = 1.394075

    var polygonObjectTypes =
        List.of(
            new PolygonObjectType(createSquare(1), USURE_LEGER),
            new PolygonObjectType(createSquare(1), MOISISSURE_NOIRCIE),
            new PolygonObjectType(createSquare(1), HUMIDITE_CLAIR));

    var subject = new AreaRateComputerFacade(roof, polygonObjectTypes);

    assertEquals(1.39, subject.getGlobalRate());
  }

  @Test
  void test_with_example_data() {
    // Surface totale du toit : 100 m²
    var roof = createSquare(10);

    // Usure légère : 10 m², Usure importante : 5 m²
    // Taux_usure : (10 × 1 + 5 × 2) / 100 = 20 %
    var usureLegere = createSquare(Math.sqrt(10));
    var usureImportante = createSquare(Math.sqrt(5));

    // Moisissure couleur : 2 m², Moisissure clair : 3 m², Moisissure noircie : 5 m²
    // Taux_moisissure : (2 + 3 + 5) / 100 = 10 %
    var mCouleur = createSquare(Math.sqrt(2));
    var mClair = createSquare(Math.sqrt(3));
    var mNoircie = createSquare(Math.sqrt(5));

    // Humidité claire : 4 m², Humidité intense : 6 m²
    // Taux_humidite : (4 × 1 + 6 × 2) / 100 = 16 %
    var hClaire = createSquare(Math.sqrt(4));
    var hIntense = createSquare(Math.sqrt(6));

    var polygonObjectTypes =
        List.of(
            new PolygonObjectType(usureLegere, USURE_LEGER),
            new PolygonObjectType(usureImportante, USURE_IMPORTANTE),
            new PolygonObjectType(mCouleur, MOISISSURE_COULEUR),
            new PolygonObjectType(mClair, MOISISSURE_CLAIR),
            new PolygonObjectType(mNoircie, MOISISSURE_NOIRCIE),
            new PolygonObjectType(hClaire, HUMIDITE_CLAIR),
            new PolygonObjectType(hIntense, HUMIDITE_INTENSE));

    var subject = new AreaRateComputerFacade(roof, polygonObjectTypes);

    assertEquals(20.0, subject.getUsureAreaRate());
    assertEquals(10.0, subject.getMoisissureAreaRate());
    assertEquals(16.0, subject.getHumidityAreaRate());

    // Moisissure amortie : mEff = 10 * (0.55 + 0.45 * 10/100) = 5.95
    // Humidite renforcee : hEff = 16 (<= 20, pas de renforcement)
    // Score = 0.55 * 20 + 0.35 * 5.95 + 0.65 * 16 = 11 + 2.0825 + 10.4 = 23.4825
    assertEquals(23.48, subject.getGlobalRate());
  }

  @Test
  void get_rate_returns_correct_enum_values() {
    var roof = createSquare(10);

    // Rate A: global < 6
    assertEquals(Rate.A, new AreaRateComputerFacade(roof, List.of()).getRate());

    // Rate B: 6 <= global < 15
    // Humidite 10% -> hEff = 10 (<= 20) -> global = 0.65 * 10 = 6.5
    assertEquals(
        Rate.B,
        new AreaRateComputerFacade(
                roof, List.of(new PolygonObjectType(createSquare(Math.sqrt(10)), HUMIDITE_CLAIR)))
            .getRate());

    // Rate C: 15 <= global < 39
    // Humidite 25% -> hEff = 25 + 0.5 * (25 - 20) = 27.5 -> global = 0.65 * 27.5 = 17.875
    assertEquals(
        Rate.C,
        new AreaRateComputerFacade(
                roof, List.of(new PolygonObjectType(createSquare(5), HUMIDITE_CLAIR)))
            .getRate());

    // Rate D: 39 <= global < 69
    // Humidite 50% -> hEff = 50 + 0.5 * (50 - 20) = 65 -> global = 0.65 * 65 = 42.25
    assertEquals(
        Rate.D,
        new AreaRateComputerFacade(
                roof, List.of(new PolygonObjectType(createSquare(Math.sqrt(50)), HUMIDITE_CLAIR)))
            .getRate());

    // Rate E: global >= 69
    // Humidite 80% -> hEff = 80 + 0.5 * (80 - 20) = 110 -> global = 0.65 * 110 = 71.5
    assertEquals(
        Rate.E,
        new AreaRateComputerFacade(
                roof, List.of(new PolygonObjectType(createSquare(Math.sqrt(80)), HUMIDITE_CLAIR)))
            .getRate());
  }

  @Test
  void format_rounds_to_two_decimal_places() {
    assertEquals(1.23, AreaRateComputerFacade.format(1.2345));
    assertEquals(1.24, AreaRateComputerFacade.format(1.2355));
    assertEquals(1.0, AreaRateComputerFacade.format(1.001));
  }

  @Test
  void constructor_with_detected_tile_should_initialize_computers_correctly() {
    var roof = createSquare(10); // Area = 100

    var usureLeger = createSquare(1); // Area = 1
    var mNoircie = createSquare(2); // Area = 4
    var hClair = createSquare(3); // Area = 9

    var detectedObjects =
        List.of(
            createDetectedObject(usureLeger, USURE_LEGER),
            createDetectedObject(mNoircie, MOISISSURE_NOIRCIE),
            createDetectedObject(hClair, HUMIDITE_CLAIR));

    DetectedTile detectedTile = DetectedTile.builder().detectedObjects(detectedObjects).build();

    var subject = new AreaRateComputerFacade(roof, detectedTile);

    // Usure: (1 * 1) / 100 * 100 = 1.0
    // Moisissure: (1 * 4) / 100 * 100 = 4.0
    // Humidite: (1 * 9) / 100 * 100 = 9.0
    // Moisissure amortie : mEff = 4 * (0.55 + 0.45 * 4/100) = 2.272
    // Humidite renforcee : hEff = 9 (<= 20, pas de renforcement)
    // Score = 0.55 * 1 + 0.35 * 2.272 + 0.65 * 9 = 0.55 + 0.7952 + 5.85 = 7.1952

    assertEquals(1.0, subject.getUsureAreaRate());
    assertEquals(4.0, subject.getMoisissureAreaRate());
    assertEquals(9.0, subject.getHumidityAreaRate());
    assertEquals(7.2, subject.getGlobalRate());
  }
}
