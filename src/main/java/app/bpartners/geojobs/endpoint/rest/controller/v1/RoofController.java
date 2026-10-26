package app.bpartners.geojobs.endpoint.rest.controller.v1;

import app.bpartners.geojobs.endpoint.rest.V1RestController;
import app.bpartners.geojobs.endpoint.rest.controller.v1.mapper.RoofScoreMapper;
import app.bpartners.geojobs.endpoint.rest.model.RoofScore;
import app.bpartners.geojobs.model.geometry.area.rate.RoofDamageRates;
import app.bpartners.geojobs.validator.RoofDamageRateValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@V1RestController
@RequiredArgsConstructor
public class RoofController {
  private final RoofScoreMapper mapper;
  private final RoofDamageRateValidator roofDamageRateValidator;

  @GetMapping("/roof/overallScore")
  public RoofScore computeRoofOverallScore(
      @RequestParam(required = false) Double humiditeRate,
      @RequestParam(required = false) Double usureRate,
      @RequestParam(required = false) Double moisissureRate) {
    RoofDamageRates roofDamageRates = new RoofDamageRates(humiditeRate, usureRate, moisissureRate);
    roofDamageRateValidator.accept(roofDamageRates);

    return mapper.toDomain(roofDamageRates);
  }
}
