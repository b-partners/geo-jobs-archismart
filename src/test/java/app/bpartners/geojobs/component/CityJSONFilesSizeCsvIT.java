package app.bpartners.geojobs.component;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import app.bpartners.geojobs.conf.FacadeIT;
import app.bpartners.geojobs.file.bucket.BucketConf;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;

@Slf4j
@Disabled("Local use only: requires AWS credentials and access to the target bucket")
class CityJSONFilesSizeCsvIT extends FacadeIT {
  private static final String KEYS_RESOURCE = "/cityjson/city_json_s3_buket_key.csv";
  private static final String CSV_HEADER = "s3_key,size_bytes";

  @Autowired private BucketConf bucketConf;

  @SneakyThrows
  @Test
  void all_cityjson_files_exist_and_sizes_are_exported_to_csv() {
    var bucketName =
        Optional.ofNullable(System.getenv("CITYJSON_BUCKET")).orElse(bucketConf.getBucketName());
    var keyPrefix = Optional.ofNullable(System.getenv("CITYJSON_KEY_PREFIX")).orElse("");
    var outputPath =
        Path.of(
            Optional.ofNullable(System.getenv("OUTPUT_CSV"))
                .orElse("build/cityjson_files_size_" + System.currentTimeMillis() + ".csv"));
    var keys = readKeys();
    List<String> lines = new ArrayList<>();
    List<String> missingKeys = new ArrayList<>();
    lines.add(CSV_HEADER);

    for (String key : keys) {
      var size = fileSize(bucketName, keyPrefix + key);
      if (size.isEmpty()) {
        missingKeys.add(key);
      }
      lines.add(key + "," + size.map(String::valueOf).orElse(""));
    }

    if (outputPath.getParent() != null) {
      Files.createDirectories(outputPath.getParent());
    }
    Files.write(outputPath, lines);
    log.info("CSV written to {}", outputPath.toAbsolutePath());

    assertEquals(keys.size() + 1, lines.size());
    assertTrue(missingKeys.isEmpty(), "Missing files in bucket " + bucketName + ": " + missingKeys);
  }

  private Optional<Long> fileSize(String bucketName, String key) {
    try {
      var response =
          bucketConf
              .getS3Client()
              .headObject(HeadObjectRequest.builder().bucket(bucketName).key(key).build());
      return Optional.of(response.contentLength());
    } catch (NoSuchKeyException e) {
      return Optional.empty();
    }
  }

  @SneakyThrows
  private List<String> readKeys() {
    try (InputStream inputStream = getClass().getResourceAsStream(KEYS_RESOURCE)) {
      return new String(inputStream.readAllBytes(), UTF_8)
          .lines()
          .map(String::trim)
          .filter(line -> !line.isEmpty())
          .toList();
    }
  }
}
