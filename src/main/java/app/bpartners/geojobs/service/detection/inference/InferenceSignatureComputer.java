package app.bpartners.geojobs.service.detection.inference;

import app.bpartners.geojobs.endpoint.rest.model.DetectableObjectModel;
import app.bpartners.geojobs.repository.ModelCardVersionRepository;
import app.bpartners.geojobs.repository.model.detection.DetectableObjectConfiguration;
import app.bpartners.geojobs.repository.model.detection.ModelCardVersion;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class InferenceSignatureComputer {
  public static final int DEFAULT_MODEL_CARD_VERSION = 1;

  private final ModelCardResolver modelCardResolver;
  private final ModelCardVersionRepository modelCardVersionRepository;

  public Optional<String> compute(
      List<DetectableObjectConfiguration> objectConfigurations, Double spatialResolutionInCm) {
    return compute(List.of(), objectConfigurations, spatialResolutionInCm);
  }

  public Optional<String> compute(
      List<DetectableObjectModel> models,
      List<DetectableObjectConfiguration> objectConfigurations,
      Double spatialResolutionInCm) {
    if (objectConfigurations == null || objectConfigurations.isEmpty()) {
      return Optional.empty();
    }
    var mappings =
        modelCardResolver.mappingsUsedBy(models, objectConfigurations, spatialResolutionInCm);
    if (mappings.isEmpty()) {
      return Optional.empty();
    }
    var modelCardIds = mappings.stream().map(ModelCardMapping::modelCardId).distinct().toList();
    var versionByModelCardId =
        modelCardVersionRepository.findLatestByModelCardIdIn(modelCardIds).stream()
            .collect(
                Collectors.toMap(
                    ModelCardVersion::getModelCardId,
                    ModelCardVersion::getVersion,
                    Math::max,
                    HashMap::new));
    return Optional.of(
        mappings.stream()
            .map(mapping -> entryOf(mapping, versionByModelCardId))
            .distinct()
            .sorted()
            .collect(Collectors.joining(";")));
  }

  private static String entryOf(ModelCardMapping mapping, Map<String, Integer> versions) {
    return mapping.modelCardId()
        + "@"
        + mapping.spatialResolutionSentToInference()
        + "#v"
        + versions.getOrDefault(mapping.modelCardId(), DEFAULT_MODEL_CARD_VERSION);
  }
}
