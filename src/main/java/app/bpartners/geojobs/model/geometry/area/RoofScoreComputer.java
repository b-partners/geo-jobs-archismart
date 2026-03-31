package app.bpartners.geojobs.model.geometry.area;

import static app.bpartners.geojobs.model.geometry.area.Rate.*;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RoofScoreComputer {
  public double getGlobalRate(RoofDamageRates roofDamageRates) {
    return roofDamageRates.humiditeRate() * HumiditeAreaRateComputer.WEIGHT
        + roofDamageRates.usureRate() * UsureAreaRateComputer.WEIGHT
        + roofDamageRates.moisissureRate() * MoisissureAreaRateComputer.WEIGHT;
  }

  public Rate getRate(double globalRate) {
    if (globalRate < 4) {
      return A;
    }
    if (globalRate >= 4 && globalRate < 11) {
      return B;
    }
    if (globalRate >= 11 && globalRate < 21) {
      return C;
    }
    if (globalRate >= 21 && globalRate < 41) {
      return D;
    }
    return E;
  }
}
