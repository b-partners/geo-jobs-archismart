package app.bpartners.geojobs.model.geometry.area.score;

import static app.bpartners.geojobs.model.geometry.area.Rate.*;

import app.bpartners.geojobs.model.geometry.area.Rate;
import app.bpartners.geojobs.model.geometry.area.rate.RoofDamageRates;
import app.bpartners.geojobs.service.area.mutation.model.MutationType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Score de degradation visible d'une toiture - methodologie V2.2.
 *
 * <p>Le socle usure/moisissure/humidite est toujours pris en compte. Revetement, monitoring, pente
 * et vegetation sont optionnels : une donnee absente vaut un coefficient neutre (1.00) ou un malus
 * nul, jamais une penalite.
 */
@Component
@RequiredArgsConstructor
public class RoofScoreComputer {
  public static final double WEIGHT_USURE = 0.55;
  public static final double WEIGHT_MOISISSURE = 0.35;
  public static final double WEIGHT_HUMIDITE = 0.65;

  public static final double THRESHOLD_GOOD = 6.0;
  public static final double THRESHOLD_PREVENTIVE = 15.0;
  public static final double THRESHOLD_INTERVENTION = 39.0;
  public static final double THRESHOLD_PRIORITY_REPAIR = 69.0;

  public double getGlobalRate(RoofDamageRates roofDamageRates) {
    double u = clamp(roofDamageRates.usureRate());
    double m = clamp(roofDamageRates.moisissureRate());
    double h = clamp(roofDamageRates.humiditeRate());

    var revetement = roofDamageRates.revetement();
    double kUsure = revetement == null ? 1.0 : revetement.kUsure;
    double kMoisissure = revetement == null ? 1.0 : revetement.kMoisissure;
    double kHumidite = revetement == null ? 1.0 : revetement.kHumidite;

    // Moisissure amortie : reste visible sans dominer artificiellement le score.
    double mEff = m * (0.55 + 0.45 * (m / 100.0));
    // Humidite renforcee au-dela de 20%.
    double hEff = h <= 20.0 ? h : h + 0.50 * (h - 20.0);

    double kPenteHumidite = slopeHumidityFactor(roofDamageRates.penteDegres(), h);

    double score =
        WEIGHT_USURE * u * kUsure
            + WEIGHT_MOISISSURE * mEff * kMoisissure
            + WEIGHT_HUMIDITE * hEff * kHumidite * kPenteHumidite
            + mutationMalus(roofDamageRates.monitoring())
            + vegetationMalus(roofDamageRates.vegetationFeu(), m, h);

    return clamp(score);
  }

  public Rate getRate(double globalRate) {
    if (globalRate < THRESHOLD_GOOD) {
      return A;
    }
    if (globalRate < THRESHOLD_PREVENTIVE) {
      return B;
    }
    if (globalRate < THRESHOLD_INTERVENTION) {
      return C;
    }
    if (globalRate < THRESHOLD_PRIORITY_REPAIR) {
      return D;
    }
    return E;
  }

  private static double slopeHumidityFactor(Double penteDegres, double humidite) {
    if (penteDegres == null) {
      return 1.00;
    }
    double pente = Math.max(0.0, penteDegres);

    if (humidite >= 10.0 && pente < 5.0) {
      return 1.25;
    }
    if (humidite >= 10.0 && pente < 10.0) {
      return 1.15;
    }
    if (humidite >= 20.0 && pente < 20.0) {
      return 1.08;
    }
    return 1.00;
  }

  private static double mutationMalus(MutationType monitoring) {
    return monitoring == MutationType.DETERIORATION ? 5.0 : 0.0;
  }

  private static double vegetationMalus(Boolean vegetationFeu, double moisissure, double humidite) {
    if (vegetationFeu == null || !vegetationFeu) {
      return 0.0;
    }
    if (humidite >= 20.0) {
      return 3.0;
    }
    if (moisissure >= 25.0) {
      return 2.0;
    }
    return 0.0;
  }

  private static double clamp(double value) {
    return Math.clamp(value, 0.0, 100.0);
  }
}
