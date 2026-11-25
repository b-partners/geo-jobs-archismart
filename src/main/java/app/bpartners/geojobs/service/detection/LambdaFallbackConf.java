package app.bpartners.geojobs.service.detection;

import static java.nio.file.Files.deleteIfExists;
import static java.nio.file.Files.readString;

import app.bpartners.geojobs.file.bucket.CustomBucketComponent;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.SneakyThrows;
import org.springframework.stereotype.Component;

@Component
public class LambdaFallbackConf {
  private static final String LAMBDA_FALLBACK_CONF_KEY = "conf/%s/lambdaFallback.json";

  private final CustomBucketComponent bucketComponent;
  private final ObjectMapper om;
  private final String env = System.getenv("ENV");
  private final Map<String, List<TileDetectorUrl>> tileDetectorUrlsByKey =
      new ConcurrentHashMap<>();

  public LambdaFallbackConf(CustomBucketComponent bucketComponent, ObjectMapper om) {
    this.bucketComponent = bucketComponent;
    this.om = om;
  }

  public List<TileDetectorUrl> getTileDetectorUrls() {
    return tileDetectorUrlsByKey.computeIfAbsent(
        String.format(LAMBDA_FALLBACK_CONF_KEY, env), this::load);
  }

  @SneakyThrows
  private List<TileDetectorUrl> load(String bucketKey) {
    var file = bucketComponent.download(bucketComponent.getBucketConf().getBucketName(), bucketKey);
    try {
      List<TileDetectorUrl> urls =
          om.readValue(readString(file.toPath()), new TypeReference<>() {});
      return List.copyOf(urls);
    } finally {
      deleteIfExists(file.toPath());
    }
  }
}
