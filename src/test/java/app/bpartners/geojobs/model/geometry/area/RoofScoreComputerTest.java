package app.bpartners.geojobs.model.geometry.area;

import static app.bpartners.geojobs.repository.model.detection.RoofCoveringType.ROOF_ZINC;
import static app.bpartners.geojobs.service.area.mutation.model.MutationType.BACKGROUND;
import static app.bpartners.geojobs.service.area.mutation.model.MutationType.DETERIORATION;
import static app.bpartners.geojobs.service.area.mutation.model.MutationType.RAS;
import static org.junit.jupiter.api.Assertions.assertEquals;

import app.bpartners.geojobs.model.geometry.area.rate.RoofDamageRates;
import app.bpartners.geojobs.model.geometry.area.score.RoofScoreComputer;
import org.junit.jupiter.api.Test;

class RoofScoreComputerTest {
  private static final double DELTA = 0.0001;

  private final RoofScoreComputer subject = new RoofScoreComputer();

  @Test
  void socle_uses_new_v22_weights() {
    // Score = 0.55 * 10 + 0.35 * mEff(0) + 0.65 * hEff(0) = 5.5
    var rates = new RoofDamageRates(0.0, 10.0, 0.0);

    assertEquals(5.5, subject.getGlobalRate(rates), DELTA);
  }

  @Test
  void missing_optional_data_never_adds_a_penalty() {
    var withoutOptionalData = new RoofDamageRates(10.0, 10.0, 10.0);
    var withOptionalDataExplicitlyAbsent =
        new RoofDamageRates(10.0, 10.0, 10.0, null, null, null, null);

    assertEquals(
        subject.getGlobalRate(withoutOptionalData),
        subject.getGlobalRate(withOptionalDataExplicitlyAbsent),
        DELTA);
  }

  @Test
  void moisissure_is_amortized_and_does_not_dominate() {
    // mEff = 50 * (0.55 + 0.45 * 50/100) = 50 * 0.775 = 38.75 ; Score = 0.35 * 38.75 = 13.5625
    var rates = new RoofDamageRates(0.0, 0.0, 50.0);

    assertEquals(13.5625, subject.getGlobalRate(rates), DELTA);
  }

  @Test
  void humidite_is_not_reinforced_at_or_below_20_percent() {
    // hEff = 20 (no reinforcement) ; Score = 0.65 * 20 = 13.0
    var rates = new RoofDamageRates(20.0, 0.0, 0.0);

    assertEquals(13.0, subject.getGlobalRate(rates), DELTA);
  }

  @Test
  void humidite_is_reinforced_above_20_percent() {
    // hEff = 40 + 0.5 * (40 - 20) = 50 ; Score = 0.65 * 50 = 32.5
    var rates = new RoofDamageRates(40.0, 0.0, 0.0);

    assertEquals(32.5, subject.getGlobalRate(rates), DELTA);
  }

  @Test
  void revetement_coefficients_apply_when_present() {
    // Zinc : kUsure = 0.95 ; Score = 0.55 * 10 * 0.95 = 5.225
    var rates = new RoofDamageRates(0.0, 10.0, 0.0, ROOF_ZINC, null, null, null);

    assertEquals(5.225, subject.getGlobalRate(rates), DELTA);
  }

  @Test
  void pente_absent_never_aggravates_humidite() {
    // hEff = 15 (<= 20) ; Score = 0.65 * 15 = 9.75, meme avec une humidite elevee
    var rates = new RoofDamageRates(15.0, 0.0, 0.0);

    assertEquals(9.75, subject.getGlobalRate(rates), DELTA);
  }

  @Test
  void steep_slope_below_5_degrees_aggravates_humidite_most() {
    // hEff = 15 ; kPenteHumidite = 1.25 (humidite >= 10 et pente < 5) ; Score = 0.65*15*1.25 =
    // 12.1875
    var rates = new RoofDamageRates(15.0, 0.0, 0.0, null, null, 3.0, null);

    assertEquals(12.1875, subject.getGlobalRate(rates), DELTA);
  }

  @Test
  void slope_between_5_and_10_degrees_aggravates_humidite_moderately() {
    // kPenteHumidite = 1.15 (humidite >= 10 et pente < 10) ; Score = 0.65*15*1.15 = 11.2125
    var rates = new RoofDamageRates(15.0, 0.0, 0.0, null, null, 7.0, null);

    assertEquals(11.2125, subject.getGlobalRate(rates), DELTA);
  }

  @Test
  void slope_below_10_degrees_does_not_aggravate_low_humidite() {
    // humidite < 10 : la pente ne s'applique pas, kPenteHumidite reste neutre
    var rates = new RoofDamageRates(5.0, 0.0, 0.0, null, null, 3.0, null);

    assertEquals(0.65 * 5.0, subject.getGlobalRate(rates), DELTA);
  }

  @Test
  void mutation_deterioration_adds_five_points() {
    var deterioration = new RoofDamageRates(0.0, 0.0, 0.0, null, DETERIORATION, null, null);
    var stable = new RoofDamageRates(0.0, 0.0, 0.0, null, RAS, null, null);
    var background = new RoofDamageRates(0.0, 0.0, 0.0, null, BACKGROUND, null, null);

    assertEquals(5.0, subject.getGlobalRate(deterioration), DELTA);
    assertEquals(0.0, subject.getGlobalRate(stable), DELTA);
    assertEquals(0.0, subject.getGlobalRate(background), DELTA);
  }

  @Test
  void vegetation_feu_with_high_humidite_adds_three_points() {
    // Score = 0.65 * hEff(25 -> 27.5) + 3 = 17.875 + 3 = 20.875
    var rates = new RoofDamageRates(25.0, 0.0, 0.0, null, null, null, true);

    assertEquals(20.875, subject.getGlobalRate(rates), DELTA);
  }

  @Test
  void vegetation_feu_with_high_moisissure_and_low_humidite_adds_two_points() {
    // mEff(30) = 30 * (0.55 + 0.45*0.3) = 20.55 ; Score = 0.35*20.55 + 2 = 7.1925 + 2 = 9.1925
    var rates = new RoofDamageRates(0.0, 0.0, 30.0, null, null, null, true);

    assertEquals(9.1925, subject.getGlobalRate(rates), DELTA);
  }

  @Test
  void vegetation_feu_absent_or_false_never_adds_a_penalty() {
    var explicitlyAbsent = new RoofDamageRates(25.0, 0.0, 0.0, null, null, null, null);
    var explicitlyFalse = new RoofDamageRates(25.0, 0.0, 0.0, null, null, null, false);

    assertEquals(
        subject.getGlobalRate(explicitlyAbsent), subject.getGlobalRate(explicitlyFalse), DELTA);
  }

  @Test
  void score_is_clamped_to_100() {
    var rates = new RoofDamageRates(100.0, 100.0, 100.0, ROOF_ZINC, DETERIORATION, 2.0, true);

    assertEquals(100.0, subject.getGlobalRate(rates), DELTA);
  }

  @Test
  void get_rate_follows_v22_ux_thresholds() {
    assertEquals(Rate.A, subject.getRate(5.99));
    assertEquals(Rate.B, subject.getRate(6.0));
    assertEquals(Rate.B, subject.getRate(14.99));
    assertEquals(Rate.C, subject.getRate(15.0));
    assertEquals(Rate.C, subject.getRate(38.99));
    assertEquals(Rate.D, subject.getRate(39.0));
    assertEquals(Rate.D, subject.getRate(68.99));
    assertEquals(Rate.E, subject.getRate(69.0));
  }
}
