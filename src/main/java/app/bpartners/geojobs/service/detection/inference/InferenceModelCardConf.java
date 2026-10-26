package app.bpartners.geojobs.service.detection.inference;

import static java.nio.file.Files.deleteIfExists;
import static java.nio.file.Files.readString;

import app.bpartners.geojobs.file.bucket.BucketComponent;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashMap;
import java.util.List;
import lombok.SneakyThrows;
import org.springframework.stereotype.Component;

@Component
public class InferenceModelCardConf {
  private static final String BUCKET_KEY_INFERENCE_MODEL_CARDS_JSON =
      "conf/%s/inferenceModelCards.json";

  private final BucketComponent bucketComponent;
  private final ObjectMapper objectMapper;
  private final String env = System.getenv("ENV");
  private volatile List<ModelCardMapping> mappings;

  public InferenceModelCardConf(BucketComponent bucketComponent, ObjectMapper objectMapper) {
    this.bucketComponent = bucketComponent;
    this.objectMapper = objectMapper;
  }

  public List<ModelCardMapping> getMappings() {
    var local = mappings;
    if (local == null) {
      synchronized (this) {
        local = mappings;
        if (local == null) {
          local = loadFromS3();
          mappings = local;
        }
      }
    }
    return local;
  }

  private static void validate(List<ModelCardMapping> mappings) {
    var firstMappingByModelCardId = new HashMap<String, ModelCardMapping>();
    var firstMappingByTypeAndResolution = new HashMap<List<Object>, ModelCardMapping>();
    for (var mapping : mappings) {
      if (mapping.zoomLevel() == null || mapping.zoomLevel() <= 0) {
        throw new IllegalStateException("Invalid zoomLevel in inference conf: " + mapping);
      }
      if (mapping.spatialResolution() == null || mapping.spatialResolution() <= 0) {
        throw new IllegalStateException("Invalid spatialResolution in inference conf: " + mapping);
      }
      var first = firstMappingByModelCardId.putIfAbsent(mapping.modelCardId(), mapping);
      if (first != null
          && (!first.zoomLevel().equals(mapping.zoomLevel())
              || !first.spatialResolution().equals(mapping.spatialResolution()))) {
        throw new IllegalStateException(
            "Model card "
                + mapping.modelCardId()
                + " is mapped with different zoomLevel or spatialResolution: "
                + first
                + " and "
                + mapping);
      }
      var duplicate =
          firstMappingByTypeAndResolution.putIfAbsent(
              List.of(mapping.objectType(), mapping.spatialResolution()), mapping);
      if (duplicate != null) {
        throw new IllegalStateException(
            "Object type "
                + mapping.objectType()
                + " is mapped several times for spatialResolution "
                + mapping.spatialResolution()
                + ": "
                + duplicate
                + " and "
                + mapping);
      }
    }
  }

  @SneakyThrows
  private List<ModelCardMapping> loadFromS3() {
    var file = bucketComponent.download(String.format(BUCKET_KEY_INFERENCE_MODEL_CARDS_JSON, env));
    try {
      List<ModelCardMapping> parsed =
          objectMapper.readValue(readString(file.toPath()), new TypeReference<>() {});
      validate(parsed);
      return List.copyOf(parsed);
    } finally {
      deleteIfExists(file.toPath());
    }
  }
}
