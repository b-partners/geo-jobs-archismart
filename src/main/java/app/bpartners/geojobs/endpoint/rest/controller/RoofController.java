package app.bpartners.geojobs.endpoint.rest.controller;

import app.bpartners.geojobs.endpoint.rest.controller.mapper.RoofScoreMapper;
import app.bpartners.geojobs.endpoint.rest.model.RoofScore;
import app.bpartners.geojobs.endpoint.rest.validator.RoofDamageRateValidator;
import app.bpartners.geojobs.model.geometry.area.RoofDamageRates;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
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
