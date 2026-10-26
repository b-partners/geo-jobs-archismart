package app.bpartners.geojobs.endpoint.rest.controller;

import static app.bpartners.geojobs.endpoint.rest.model.RoofScoreCategory.C;
import static org.junit.jupiter.api.Assertions.*;

import app.bpartners.geojobs.endpoint.rest.controller.v1.RoofController;
import app.bpartners.geojobs.endpoint.rest.controller.v1.mapper.RoofScoreMapper;
import app.bpartners.geojobs.endpoint.rest.controller.v1.mapper.cityjson.RoofScoreCategoryMapper;
import app.bpartners.geojobs.model.exception.BadRequestException;
import app.bpartners.geojobs.model.geometry.area.score.RoofScoreComputer;
import app.bpartners.geojobs.validator.RoofDamageRateValidator;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class RoofControllerTest {
  RoofScoreComputer scoreComputer = new RoofScoreComputer();
  RoofDamageRateValidator conditionValidator = new RoofDamageRateValidator();
  RoofScoreCategoryMapper categoryMapper = new RoofScoreCategoryMapper();
  RoofScoreMapper mapper = new RoofScoreMapper(categoryMapper, scoreComputer);
  RoofController subject = new RoofController(mapper, conditionValidator);

  @Test
  void rate_compute_ok() {
    var actual = subject.computeRoofOverallScore(10.0, 20.0, 30.0);

    // Moisissure amortie : mEff = 30 * (0.55 + 0.45 * 30/100) = 20.55
    // Humidite non renforcee (<= 20) : hEff = 10
    // Score = 0.55 * 20 + 0.35 * 20.55 + 0.65 * 10 = 11 + 7.1925 + 6.5 = 24.6925
    assertEquals(BigDecimal.valueOf(24.6925), actual.getScore());
    assertEquals(C, actual.getCategory());
  }

  @Test
  void negative_rate_ko() {
    var actual =
        assertThrows(
            BadRequestException.class, () -> subject.computeRoofOverallScore(-1.0, 20.0, 30.0));

    assertEquals("Rates must be positive", actual.getMessage());
  }

  @Test
  void sum_rate_ko() {
    var actual =
        assertThrows(
            BadRequestException.class, () -> subject.computeRoofOverallScore(100.0, 20.0, 40.0));

    assertEquals("Sum of rates must not exceed 100, actual : " + 160.0, actual.getMessage());
  }
}
