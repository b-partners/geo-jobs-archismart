package app.bpartners.geojobs.service.detection.inference;

import app.bpartners.geojobs.repository.model.ArcgisImageZoom;
import app.bpartners.geojobs.repository.model.detection.DetectableObjectConfiguration;
import app.bpartners.geojobs.repository.model.detection.DetectableType;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class ModelCardResolver {
  public static final int DEFAULT_TILING_ZOOM = ArcgisImageZoom.HOUSES_0.getZoomLevel();
  public static final double DEFAULT_SPATIAL_RESOLUTION_IN_CM = 5.0;

  private final InferenceModelCardConf conf;
  private final ModelCardRegistry registry;

  private final boolean inferenceApiEnabled;

  @Autowired
  public ModelCardResolver(
      InferenceModelCardConf conf,
      ModelCardRegistry registry,
      @Value("${detection.inference.api.enabled}") boolean inferenceApiEnabled) {
    this.conf = conf;
    this.registry = registry;
    this.inferenceApiEnabled = inferenceApiEnabled;
  }

  public ModelCardResolver(InferenceModelCardConf conf, ModelCardRegistry registry) {
    this(conf, registry, true);
  }

  public boolean isInferenceApiEnabled() {
    return inferenceApiEnabled;
  }

  public int tilingZoomOf(DetectableType objectType, Double spatialResolutionInCm) {
    return mappingOf(mappingsByType(), objectType, spatialResolutionInCm)
        .map(ModelCardMapping::zoomLevel)
        .orElse(DEFAULT_TILING_ZOOM);
  }

  public Set<Double> supportedSpatialResolutionsInCm(
      List<DetectableObjectConfiguration> objectConfigurations) {
    return supportedSpatialResolutionsInCmOfTypes(
        objectConfigurations.stream().map(DetectableObjectConfiguration::getObjectType).toList());
  }

  public Set<Double> supportedSpatialResolutionsInCmOfTypes(Collection<DetectableType> types) {
    var mappingsByType = mappingsByType();
    Set<Double> supported = null;
    for (var type : types) {
      var typeResolutions = resolutionsOf(mappingsByType, type);
      if (supported == null) {
        supported = new LinkedHashSet<>(typeResolutions);
      } else {
        supported.retainAll(typeResolutions);
      }
    }
    return supported == null ? Set.of(DEFAULT_SPATIAL_RESOLUTION_IN_CM) : supported;
  }

  public List<Integer> tilingZoomLevels(
      List<DetectableObjectConfiguration> objectConfigurations, Double spatialResolutionInCm) {
    var mappingsByType = mappingsByType();
    var zoomLevels =
        objectConfigurations.stream()
            .map(
                configuration ->
                    mappingOf(mappingsByType, configuration.getObjectType(), spatialResolutionInCm)
                        .map(ModelCardMapping::zoomLevel)
                        .orElse(DEFAULT_TILING_ZOOM))
            .distinct()
            .sorted()
            .toList();
    return zoomLevels.isEmpty() ? List.of(DEFAULT_TILING_ZOOM) : zoomLevels;
  }

  public Resolution resolve(
      List<DetectableObjectConfiguration> objectConfigurations,
      Double spatialResolutionInCm,
      int tileZoom,
      Predicate<DeployedModelCard> compatible) {
    var mappingsByType = mappingsByType();
    var modelCardsById = new LinkedHashMap<String, DeployedModelCard>();
    var usableModelCardsById = new LinkedHashMap<String, ResolvedModelCard>();
    var unmappedTypes = new LinkedHashSet<DetectableType>();
    var unsupportedTypes = new LinkedHashSet<DetectableType>();
    for (var objectConfiguration : objectConfigurations) {
      var objectType = objectConfiguration.getObjectType();
      var mappingFound = mappingOf(mappingsByType, objectType, spatialResolutionInCm);
      if (mappingFound.isEmpty()) {
        if (tileZoom == DEFAULT_TILING_ZOOM) {
          unmappedTypes.add(objectType);
        }
        continue;
      }
      var mapping = mappingFound.get();
      if (mapping.zoomLevel() != tileZoom) {
        continue;
      }
      var modelCard =
          modelCardsById.computeIfAbsent(mapping.modelCardId(), id -> findRunnable(objectType, id));
      if (compatible.test(modelCard)) {
        usableModelCardsById.putIfAbsent(
            mapping.modelCardId(),
            new ResolvedModelCard(modelCard, mapping.zoomLevel(), mapping.spatialResolution()));
      } else {
        unsupportedTypes.add(objectType);
      }
    }
    return new Resolution(
        List.copyOf(usableModelCardsById.values()),
        Set.copyOf(unmappedTypes),
        Set.copyOf(unsupportedTypes));
  }

  private Map<DetectableType, List<ModelCardMapping>> mappingsByType() {
    var mappingsByType = new LinkedHashMap<DetectableType, List<ModelCardMapping>>();
    if (!inferenceApiEnabled) {
      return mappingsByType;
    }
    conf.getMappings()
        .forEach(
            mapping ->
                mappingsByType
                    .computeIfAbsent(mapping.objectType(), type -> new ArrayList<>())
                    .add(mapping));
    return mappingsByType;
  }

  private static Optional<ModelCardMapping> mappingOf(
      Map<DetectableType, List<ModelCardMapping>> mappingsByType,
      DetectableType objectType,
      Double spatialResolutionInCm) {
    var mappings = mappingsByType.getOrDefault(objectType, List.of());
    if (mappings.isEmpty()) {
      return Optional.empty();
    }
    if (spatialResolutionInCm != null) {
      return withResolution(mappings, spatialResolutionInCm);
    }
    return withResolution(mappings, DEFAULT_SPATIAL_RESOLUTION_IN_CM)
        .or(() -> Optional.of(mappings.getFirst()));
  }

  private static Optional<ModelCardMapping> withResolution(
      List<ModelCardMapping> mappings, double spatialResolutionInCm) {
    return mappings.stream()
        .filter(mapping -> mapping.spatialResolution() == spatialResolutionInCm)
        .findFirst();
  }

  private static Set<Double> resolutionsOf(
      Map<DetectableType, List<ModelCardMapping>> mappingsByType, DetectableType objectType) {
    var mappings = mappingsByType.getOrDefault(objectType, List.of());
    if (mappings.isEmpty()) {
      return Set.of(DEFAULT_SPATIAL_RESOLUTION_IN_CM);
    }
    return mappings.stream()
        .map(ModelCardMapping::spatialResolution)
        .collect(Collectors.toCollection(LinkedHashSet::new));
  }

  private DeployedModelCard findRunnable(DetectableType objectType, String modelCardId) {
    return registry
        .findById(modelCardId)
        .orElseThrow(
            () ->
                new IllegalStateException(
                    "Model card "
                        + modelCardId
                        + " mapped to "
                        + objectType
                        + " is not deployed or not runnable"));
  }

  public record Resolution(
      List<ResolvedModelCard> modelCards,
      Set<DetectableType> unmappedTypes,
      Set<DetectableType> unsupportedTypes) {}
}
