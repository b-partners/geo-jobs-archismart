package app.bpartners.geojobs.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import app.bpartners.geojobs.endpoint.rest.model.DetectionFileObject;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

@Slf4j
class DetectionFileObjectTest {
  ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
  static final Path OUTPUT_DIRECTORY = Path.of("target", "downloaded-file-objects");

  @SneakyThrows
  @Test
  void download_file_objects() {
    var fileObjectFile = new ClassPathResource("/fileObjects/file_object_example.json").getFile();

    var actual =
        objectMapper.readValue(fileObjectFile, new TypeReference<List<DetectionFileObject>>() {});

    assertNotNull(actual);
    // downloadLocally(actual); TODO: local use only
  }

  private void downloadLocally(List<DetectionFileObject> actual) {
    var httpClient = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NORMAL).build();
    for (var fileObject : actual) {
      downloadFileObject(httpClient, fileObject);
    }
  }

  @SneakyThrows
  private void downloadFileObject(HttpClient httpClient, DetectionFileObject fileObject) {
    var targetFile = resolveTargetFile(fileObject.getFileName());
    Files.createDirectories(targetFile.getParent());

    var request = HttpRequest.newBuilder().uri(URI.create(fileObject.getFileUrl())).GET().build();
    var response = httpClient.send(request, HttpResponse.BodyHandlers.ofFile(targetFile));

    assertEquals(
        200,
        response.statusCode(),
        "failed to download "
            + fileObject.getFileName()
            + " (the pre-signed URL may have expired)");
  }

  /**
   * The fileName follows the {@code layers_z_x_y} convention (e.g. {@code
   * Haut-de-france_20_532432_358352.jpg}), which mirrors the S3 key. {@code layers}, {@code z} and
   * {@code x} become (sub)directories; everything after the third {@code _} (the {@code y} value
   * together with any suffix such as {@code _mask} / {@code _response_v2_...} and the {@code .jpg}
   * or {@code .json} extension) is the file name.
   */
  private Path resolveTargetFile(String fileName) {
    var parts = fileName.split("_", 4);
    var layers = parts[0];
    var z = parts[1];
    var x = parts[2];
    var y = parts[3];

    var targetFilePath = OUTPUT_DIRECTORY.resolve(Path.of(layers, z, x, y));

    log.info("Downloading {} to {}", fileName, targetFilePath);

    return targetFilePath;
  }
}
