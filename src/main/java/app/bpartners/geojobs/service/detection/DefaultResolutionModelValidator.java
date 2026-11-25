package app.bpartners.geojobs.service.detection;

import static app.bpartners.geojobs.endpoint.rest.model.ModelName.TOITURE;
import static app.bpartners.geojobs.model.exception.ApiException.ExceptionType.SERVER_EXCEPTION;

import app.bpartners.geojobs.endpoint.rest.model.ModelName;
import app.bpartners.geojobs.model.exception.ApiException;
import app.bpartners.geojobs.service.detection.inference.ModelCardResolver;
import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class DefaultResolutionModelValidator {
  private static final Set<ModelName> MODELS_REQUIRING_DEFAULT_RESOLUTION = Set.of(TOITURE);

  private final boolean toiture20CmEnabled;

  public DefaultResolutionModelValidator(
      @Value("${detection.toiture.20cm.enabled}") boolean toiture20CmEnabled) {
    this.toiture20CmEnabled = toiture20CmEnabled;
  }

  public void accept(List<ModelName> modelNames, Integer precisionLevelInCm, String layer) {
    if (toiture20CmEnabled) {
      return;
    }
    if (precisionLevelInCm != null
        && precisionLevelInCm.doubleValue() == ModelCardResolver.DEFAULT_SPATIAL_RESOLUTION_IN_CM) {
      return;
    }
    var restrictedModelNames =
        modelNames.stream().filter(MODELS_REQUIRING_DEFAULT_RESOLUTION::contains).toList();
    if (restrictedModelNames.isEmpty()) {
      return;
    }
    throw new ApiException(
        SERVER_EXCEPTION,
        String.format(
            "Cannot process detection : image resolution must be %s cm. Current layer = %s"
                + " (precisionLevelInCm = %s). Model = %s",
            BigDecimal.valueOf(ModelCardResolver.DEFAULT_SPATIAL_RESOLUTION_IN_CM)
                .stripTrailingZeros()
                .toPlainString(),
            layer,
            precisionLevelInCm,
            restrictedModelNames.stream()
                .map(ModelName::toString)
                .collect(Collectors.joining(", "))));
  }
}
