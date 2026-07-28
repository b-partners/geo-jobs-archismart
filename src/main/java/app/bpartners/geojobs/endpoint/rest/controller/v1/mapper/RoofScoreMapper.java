package app.bpartners.geojobs.endpoint.rest.controller.v1.mapper;

import app.bpartners.geojobs.endpoint.rest.controller.v1.mapper.cityjson.RoofScoreCategoryMapper;
import app.bpartners.geojobs.endpoint.rest.model.RoofScore;
import app.bpartners.geojobs.model.geometry.area.Rate;
import app.bpartners.geojobs.model.geometry.area.RoofDamageRates;
import app.bpartners.geojobs.model.geometry.area.RoofScoreComputer;
import java.math.BigDecimal;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RoofScoreMapper {
  private final RoofScoreCategoryMapper categoryMapper;
  private final RoofScoreComputer computer;

  public RoofScore toDomain(RoofDamageRates roofDamageRates) {
    double score = computer.getGlobalRate(roofDamageRates);
    Rate category = computer.getRate(score);

    return new RoofScore()
        .score(BigDecimal.valueOf(score))
        .category(categoryMapper.toRest(category));
  }
}
