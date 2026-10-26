package app.bpartners.geojobs.service;

import static app.bpartners.geojobs.endpoint.rest.controller.v1.mapper.DetectableObjectTypeMapper.detectableObjectTypeForToitureModel;

import app.bpartners.geojobs.endpoint.rest.model.DetectableObjectType;
import app.bpartners.geojobs.repository.model.detection.DetectableObjectConfiguration;
import app.bpartners.geojobs.repository.model.detection.DetectableType;
import app.bpartners.geojobs.repository.model.detection.Detection;
import app.bpartners.geojobs.service.detection.inference.ModelCardResolver;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DetectionImageZoomResolver {
  private static final Set<String> TOITURE_TYPE_NAMES =
      detectableObjectTypeForToitureModel().stream()
          .map(DetectableObjectType::name)
          .collect(Collectors.toUnmodifiableSet());

  private final ModelCardResolver modelCardResolver;

  public int apply(Detection detection) {
    var configurations = detection.getDetectableObjectConfigurations();
    if (configurations == null || configurations.isEmpty()) {
      return ModelCardResolver.DEFAULT_TILING_ZOOM;
    }
    var objectTypes =
        configurations.stream().map(DetectableObjectConfiguration::getObjectType).toList();
    var referenceTypes =
        detection.hasRoofBasedModelName() ? toitureTypes(objectTypes) : objectTypes;
    var spatialResolutionInCm = detection.spatialResolutionInCm();
    return (referenceTypes.isEmpty() ? objectTypes : referenceTypes)
        .stream()
            .mapToInt(type -> modelCardResolver.tilingZoomOf(type, spatialResolutionInCm))
            .max()
            .orElseThrow();
  }

  public int toitureZoom() {
    return TOITURE_TYPE_NAMES.stream()
        .map(DetectableType::valueOf)
        .mapToInt(type -> modelCardResolver.tilingZoomOf(type, null))
        .max()
        .orElse(ModelCardResolver.DEFAULT_TILING_ZOOM);
  }

  private static List<DetectableType> toitureTypes(List<DetectableType> objectTypes) {
    return objectTypes.stream().filter(type -> TOITURE_TYPE_NAMES.contains(type.name())).toList();
  }
}
