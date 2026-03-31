package app.bpartners.geojobs.endpoint.rest.validator;

import app.bpartners.geojobs.model.exception.BadRequestException;
import app.bpartners.geojobs.model.geometry.area.RoofDamageRates;
import java.util.function.Consumer;
import org.springframework.stereotype.Component;

@Component
public class RoofDamageRateValidator implements Consumer<RoofDamageRates> {

  @Override
  public void accept(RoofDamageRates roofDamageRates) {
    var humiditeRate = roofDamageRates.humiditeRate();
    var usureRate = roofDamageRates.usureRate();
    var moisissureRate = roofDamageRates.moisissureRate();

    if (humiditeRate == null || usureRate == null || moisissureRate == null) {
      throw new BadRequestException("humiditeRate, usureRate and moisissureRate are mandatory.");
    }

    if (humiditeRate < 0 || usureRate < 0 || moisissureRate < 0) {
      throw new BadRequestException("Rates must be positive");
    }

    var sumRate = humiditeRate + usureRate + moisissureRate;
    if (sumRate > 100) {
      throw new BadRequestException("Sum of rates must not exceed 100, actual : " + sumRate);
    }
  }
}
