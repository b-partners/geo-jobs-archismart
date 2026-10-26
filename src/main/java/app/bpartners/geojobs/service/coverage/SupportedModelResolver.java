package app.bpartners.geojobs.service.coverage;

import app.bpartners.geojobs.endpoint.rest.controller.v1.mapper.DetectableObjectTypeMapper;
import app.bpartners.geojobs.endpoint.rest.model.ModelName;
import app.bpartners.geojobs.repository.model.detection.DetectableType;
import app.bpartners.geojobs.service.detection.inference.ModelCardResolver;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SupportedModelResolver {
  private static final Map<ModelName, Set<DetectableType>> OPTIONAL_TYPES =
      Map.of(ModelName.REVETEMENT_OBSTACLE, Set.of(DetectableType.LUMINEUX));

  private final DetectableObjectTypeMapper objectTypeMapper;
  private final ModelCardResolver modelCardResolver;

  public List<ModelName> supportedModels(Integer precisionInCm) {
    if (precisionInCm == null) {
      return List.of();
    }
    return Arrays.stream(ModelName.values())
        .filter(modelName -> isSupported(modelName, precisionInCm))
        .toList();
  }

  private boolean isSupported(ModelName modelName, int precisionInCm) {
    var modelTypes =
        objectTypeMapper.mapFromModel(modelName).stream().map(objectTypeMapper::toDomain).toList();
    var requiredTypes =
        modelTypes.stream()
            .filter(
                type ->
                    !OPTIONAL_TYPES.getOrDefault(modelName, Set.of()).contains(type)
                        || supports(List.of(type), precisionInCm))
            .toList();
    return !requiredTypes.isEmpty() && supports(requiredTypes, precisionInCm);
  }

  private boolean supports(List<DetectableType> types, int precisionInCm) {
    return modelCardResolver
        .supportedSpatialResolutionsInCmOfTypes(types)
        .contains((double) precisionInCm);
  }
}
