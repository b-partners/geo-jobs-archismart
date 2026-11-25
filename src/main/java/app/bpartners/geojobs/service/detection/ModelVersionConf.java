package app.bpartners.geojobs.service.detection;

import static java.nio.file.Files.deleteIfExists;
import static java.nio.file.Files.readString;

import app.bpartners.geojobs.endpoint.rest.model.DetectableObjectModel;
import app.bpartners.geojobs.endpoint.rest.model.ModelName;
import app.bpartners.geojobs.file.bucket.CustomBucketComponent;
import app.bpartners.geojobs.model.exception.BadRequestException;
import app.bpartners.geojobs.service.detection.inference.ModelCardMapping;
import app.bpartners.geojobs.service.detection.inference.ModelCardMappingValidator;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.s3.model.S3Object;

@Component
@Slf4j
public class ModelVersionConf {
  public static final String DEFAULT_VERSION = "1.1.0";
  public static final String LEGACY_VERSION = "legacy";
  private static final String LEGACY_CONF_PREFIX = "conf/%s/model-versions/";
  private static final String MODEL_CONF_PREFIX = "conf/%s/models/%s/";
  private static final String MODEL_VERSIONS_DIRECTORY = "versions/";
  private static final String MODEL_CURRENT_POINTER = "current.json";
  private static final String CONF_FILE_EXTENSION = ".json";
  private static final Comparator<String> BY_SEMANTIC_VERSION = comparingSemanticVersion();

  private final CustomBucketComponent bucketComponent;
  private final ObjectMapper om;
  private final String env = System.getenv("ENV");
  private final Map<String, Map<ModelName, ModelConf>> legacyConfByVersion =
      new ConcurrentHashMap<>();
  private final Map<String, Set<String>> legacyVersionsByPrefix = new ConcurrentHashMap<>();
  private final Map<ModelName, ModelListing> listingByModel = new ConcurrentHashMap<>();
  private final Map<ModelVersionKey, Optional<ModelConf>> confByModelVersion =
      new ConcurrentHashMap<>();

  public ModelVersionConf(CustomBucketComponent bucketComponent, ObjectMapper om) {
    this.bucketComponent = bucketComponent;
    this.om = om;
  }

  public void warmUp() {
    var tasks = new ArrayList<Callable<Void>>();
    for (var modelName : ModelName.values()) {
      tasks.add(
          () -> {
            warm(modelName.getValue(), () -> conf(modelName, defaultVersion(modelName)));
            return null;
          });
    }
    tasks.add(
        () -> {
          warm("legacy", () -> legacyVersions().forEach(this::legacyConf));
          return null;
        });
    try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
      executor.invokeAll(tasks);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }

  private static void warm(String label, Runnable loading) {
    try {
      loading.run();
    } catch (Exception e) {
      log.warn("Could not warm up the model version conf of {}: {}", label, e.getMessage());
    }
  }

  public String defaultVersion(ModelName modelName) {
    if (modelName == null) {
      return DEFAULT_VERSION;
    }
    var current = listing(modelName).currentVersion();
    return current == null ? DEFAULT_VERSION : current;
  }

  public String resolveVersion(ModelName modelName, String requestedVersion) {
    if (requestedVersion == null || requestedVersion.isBlank()) {
      return defaultVersion(modelName);
    }
    if (LEGACY_VERSION.equals(requestedVersion)) {
      return earliestVersion(modelName);
    }
    if (!containsVersion(getAvailableVersions(modelName), requestedVersion)) {
      throw new BadRequestException("Unsupported model version " + requestedVersion);
    }
    return requestedVersion;
  }

  public boolean hasConfiguration(ModelName modelName, String requestedVersion) {
    var version =
        requestedVersion == null || requestedVersion.isBlank()
            ? defaultVersion(modelName)
            : requestedVersion;
    return containsVersion(getAvailableVersions(modelName), version);
  }

  public Set<String> getAvailableVersions(ModelName modelName) {
    var availableVersions = new TreeSet<String>(BY_SEMANTIC_VERSION);
    if (modelName == null) {
      return availableVersions;
    }
    availableVersions.addAll(listing(modelName).versions());
    for (var version : legacyVersions()) {
      if (legacyConf(version).containsKey(modelName)) {
        availableVersions.add(version);
      }
    }
    return availableVersions;
  }

  public List<TileDetectorUrl> getTileDetectorUrls(ModelName modelName, String requestedVersion) {
    var version = resolveVersion(modelName, requestedVersion);
    return conf(modelName, version)
        .orElseThrow(
            () ->
                new BadRequestException(
                    "Model "
                        + modelName.getValue()
                        + " has no configuration for version "
                        + version))
        .lambdas();
  }

  public List<ModelCardMapping> getInferenceMappings(List<DetectableObjectModel> models) {
    var selectedModels =
        models == null
            ? List.<DetectableObjectModel>of()
            : models.stream().filter(model -> model.getModelName() != null).toList();
    var mappingsByTypeAndResolution = new LinkedHashMap<List<Object>, ModelCardMapping>();
    if (selectedModels.isEmpty()) {
      for (var modelName : ModelName.values()) {
        conf(modelName, defaultVersion(modelName))
            .ifPresent(modelConf -> collect(modelConf.inference(), mappingsByTypeAndResolution));
      }
    }
    for (var model : selectedModels) {
      var version = resolveVersion(model.getModelName(), model.getModelVersion());
      conf(model.getModelName(), version)
          .ifPresent(modelConf -> collect(modelConf.inference(), mappingsByTypeAndResolution));
    }
    return List.copyOf(mappingsByTypeAndResolution.values());
  }

  private static <K, V> V cached(Map<K, V> cache, K key, Function<K, V> loader) {
    var cachedValue = cache.get(key);
    if (cachedValue != null) {
      return cachedValue;
    }
    var loaded = loader.apply(key);
    var concurrent = cache.putIfAbsent(key, loaded);
    return concurrent != null ? concurrent : loaded;
  }

  private static boolean containsVersion(Set<String> versions, String version) {
    return versions.stream().anyMatch(version::equals);
  }

  private static void collect(
      List<ModelCardMapping> mappings, Map<List<Object>, ModelCardMapping> collected) {
    mappings.forEach(
        mapping ->
            collected.putIfAbsent(
                List.of(mapping.objectType(), mapping.spatialResolution()), mapping));
  }

  private String earliestVersion(ModelName modelName) {
    return getAvailableVersions(modelName).stream()
        .min(BY_SEMANTIC_VERSION)
        .orElseThrow(
            () ->
                new BadRequestException(
                    "No model version configuration found for model "
                        + modelName.getValue()
                        + " under "
                        + String.format(MODEL_CONF_PREFIX, env, modelName.getValue())
                        + " or "
                        + String.format(LEGACY_CONF_PREFIX, env)));
  }

  private ModelListing listing(ModelName modelName) {
    return cached(listingByModel, modelName, this::listModel);
  }

  private ModelListing listModel(ModelName modelName) {
    var prefix = String.format(MODEL_CONF_PREFIX, env, modelName.getValue());
    var keys =
        bucketComponent
            .listObjects(bucketComponent.getBucketConf().getBucketName(), prefix)
            .stream()
            .map(S3Object::key)
            .map(key -> key.substring(prefix.length()))
            .toList();
    var versions =
        keys.stream()
            .filter(name -> name.startsWith(MODEL_VERSIONS_DIRECTORY))
            .map(name -> name.substring(MODEL_VERSIONS_DIRECTORY.length()))
            .filter(name -> name.endsWith(CONF_FILE_EXTENSION) && !name.contains("/"))
            .map(name -> name.substring(0, name.length() - CONF_FILE_EXTENSION.length()))
            .filter(name -> !name.isBlank())
            .collect(Collectors.toCollection(() -> new TreeSet<>(BY_SEMANTIC_VERSION)));
    var currentVersion =
        keys.contains(MODEL_CURRENT_POINTER)
            ? readCurrentVersion(prefix + MODEL_CURRENT_POINTER)
            : null;
    if (currentVersion != null && !containsVersion(versions, currentVersion)) {
      throw new IllegalStateException(
          "Current version "
              + currentVersion
              + " of model "
              + modelName.getValue()
              + " is not published under "
              + prefix
              + MODEL_VERSIONS_DIRECTORY);
    }
    return new ModelListing(versions, currentVersion);
  }

  @SneakyThrows
  private String readCurrentVersion(String bucketKey) {
    var pointer =
        om.readValue(readBucketFile(bucketKey), new TypeReference<Map<String, String>>() {});
    var version = pointer.get("version");
    if (version == null || version.isBlank()) {
      throw new IllegalStateException("Current pointer " + bucketKey + " must define a version");
    }
    return version;
  }

  private Optional<ModelConf> conf(ModelName modelName, String version) {
    return cached(confByModelVersion, new ModelVersionKey(modelName, version), this::loadModelConf);
  }

  @SneakyThrows
  private Optional<ModelConf> loadModelConf(ModelVersionKey key) {
    if (containsVersion(listing(key.modelName()).versions(), key.version())) {
      var bucketKey =
          String.format(MODEL_CONF_PREFIX, env, key.modelName().getValue())
              + MODEL_VERSIONS_DIRECTORY
              + key.version()
              + CONF_FILE_EXTENSION;
      List<ModelVersionEntry> entries =
          om.readValue(readBucketFile(bucketKey), new TypeReference<>() {});
      return Optional.of(toModelConf(entries));
    }
    return Optional.ofNullable(legacyConf(key.version()).get(key.modelName()));
  }

  private Set<String> legacyVersions() {
    return cached(
        legacyVersionsByPrefix, String.format(LEGACY_CONF_PREFIX, env), this::listLegacyVersions);
  }

  private Set<String> listLegacyVersions(String prefix) {
    return bucketComponent
        .listObjects(bucketComponent.getBucketConf().getBucketName(), prefix)
        .stream()
        .map(S3Object::key)
        .map(key -> key.substring(prefix.length()))
        .filter(name -> name.endsWith(CONF_FILE_EXTENSION) && !name.contains("/"))
        .map(name -> name.substring(0, name.length() - CONF_FILE_EXTENSION.length()))
        .filter(name -> !name.isBlank())
        .collect(Collectors.toCollection(() -> new TreeSet<>(BY_SEMANTIC_VERSION)));
  }

  private Map<ModelName, ModelConf> legacyConf(String version) {
    if (!containsVersion(legacyVersions(), version)) {
      return Map.of();
    }
    return cached(legacyConfByVersion, version, this::loadLegacy);
  }

  @SneakyThrows
  private Map<ModelName, ModelConf> loadLegacy(String version) {
    var bucketKey = String.format(LEGACY_CONF_PREFIX, env) + version + CONF_FILE_EXTENSION;
    Map<String, List<ModelVersionEntry>> rawConfByModel =
        om.readValue(readBucketFile(bucketKey), new TypeReference<>() {});

    Map<ModelName, ModelConf> confByModel = new EnumMap<>(ModelName.class);
    rawConfByModel.forEach(
        (modelName, entries) ->
            confByModel.put(ModelName.fromValue(modelName), toModelConf(entries)));
    return confByModel;
  }

  @SneakyThrows
  private String readBucketFile(String bucketKey) {
    var file = bucketComponent.download(bucketComponent.getBucketConf().getBucketName(), bucketKey);
    var content = readString(file.toPath());
    deleteIfExists(file.toPath());
    return content;
  }

  private static ModelConf toModelConf(List<ModelVersionEntry> entries) {
    var inference = new ArrayList<ModelCardMapping>();
    var lambdas = new ArrayList<TileDetectorUrl>();
    for (var entry : entries) {
      var hasModelCard = entry.modelCardId() != null;
      var hasUrl = entry.url() != null;
      if (hasModelCard == hasUrl) {
        throw new IllegalStateException(
            "Model version entry must define either modelCardId or url: " + entry);
      }
      if (hasModelCard) {
        inference.add(
            new ModelCardMapping(
                entry.objectType(),
                entry.modelCardId(),
                entry.zoomLevel(),
                entry.spatialResolution(),
                entry.inferenceSpatialResolution()));
      } else {
        lambdas.add(
            TileDetectorUrl.builder()
                .objectType(entry.objectType())
                .url(entry.url())
                .version(entry.version())
                .build());
      }
    }
    ModelCardMappingValidator.validate(inference);
    return new ModelConf(List.copyOf(inference), List.copyOf(lambdas));
  }

  private record ModelConf(List<ModelCardMapping> inference, List<TileDetectorUrl> lambdas) {}

  private record ModelListing(Set<String> versions, String currentVersion) {}

  private record ModelVersionKey(ModelName modelName, String version) {}

  private static Comparator<String> comparingSemanticVersion() {
    return (left, right) -> {
      var leftParts = Arrays.stream(left.split("\\.")).map(Integer::parseInt).toList();
      var rightParts = Arrays.stream(right.split("\\.")).map(Integer::parseInt).toList();
      for (var i = 0; i < Math.max(leftParts.size(), rightParts.size()); i++) {
        var leftPart = i < leftParts.size() ? leftParts.get(i) : 0;
        var rightPart = i < rightParts.size() ? rightParts.get(i) : 0;
        if (leftPart != rightPart) {
          return Integer.compare(leftPart, rightPart);
        }
      }
      return 0;
    };
  }
}
