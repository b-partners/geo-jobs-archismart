package app.bpartners.geojobs.service.detection;

import static app.bpartners.geojobs.endpoint.rest.model.ModelName.REVETEMENT_OBSTACLE;
import static app.bpartners.geojobs.endpoint.rest.model.ModelName.TOITURE;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import app.bpartners.geojobs.model.exception.ApiException;
import java.util.List;
import org.junit.jupiter.api.Test;

class DefaultResolutionModelValidatorTest {
  private final DefaultResolutionModelValidator enabled = new DefaultResolutionModelValidator(true);
  private final DefaultResolutionModelValidator disabled =
      new DefaultResolutionModelValidator(false);

  @Test
  void accepts_toiture_at_20_cm_when_flag_is_enabled() {
    assertDoesNotThrow(() -> enabled.accept(List.of(TOITURE), 20, "layer-20cm"));
  }

  @Test
  void accepts_toiture_with_unknown_precision_when_flag_is_enabled() {
    assertDoesNotThrow(() -> enabled.accept(List.of(TOITURE), null, "layer"));
  }

  @Test
  void refuses_toiture_at_20_cm_when_flag_is_disabled() {
    assertThrows(ApiException.class, () -> disabled.accept(List.of(TOITURE), 20, "layer-20cm"));
  }

  @Test
  void accepts_toiture_at_5_cm_when_flag_is_disabled() {
    assertDoesNotThrow(() -> disabled.accept(List.of(TOITURE), 5, "layer-5cm"));
  }

  @Test
  void accepts_other_models_at_20_cm_when_flag_is_disabled() {
    assertDoesNotThrow(() -> disabled.accept(List.of(REVETEMENT_OBSTACLE), 20, "layer-20cm"));
  }
}
