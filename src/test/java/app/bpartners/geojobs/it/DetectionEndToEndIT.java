package app.bpartners.geojobs.it;

import static app.bpartners.geojobs.endpoint.rest.model.DelimitationType.PARCEL_FREE_DELIMITATION;
import static app.bpartners.geojobs.endpoint.rest.model.GeoJsonOutput.ZIP;
import static app.bpartners.geojobs.endpoint.rest.model.ModelName.TOITURE;
import static app.bpartners.geojobs.endpoint.rest.security.authenticator.ApiKeyAuthenticator.API_KEY_HEADER;
import static app.bpartners.geojobs.utils.it.AddressPointCsvReader.DETECTION_2D_KO_CSV;
import static app.bpartners.geojobs.utils.it.AddressPointFeatures.toPointFeature;
import static app.bpartners.geojobs.utils.it.JsonArtifactWriter.sanitize;
import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import app.bpartners.geojobs.endpoint.rest.api.DetectionApi;
import app.bpartners.geojobs.endpoint.rest.client.ApiClient;
import app.bpartners.geojobs.endpoint.rest.client.ApiException;
import app.bpartners.geojobs.endpoint.rest.model.*;
import app.bpartners.geojobs.utils.it.AddressPoint;
import app.bpartners.geojobs.utils.it.AddressPointCsvReader;
import app.bpartners.geojobs.utils.it.JsonArtifactWriter;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.http.HttpClient;
import java.nio.file.Path;
import java.util.List;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Disabled("Local use only")
class DetectionEndToEndIT {
  private static final Logger log = LoggerFactory.getLogger(DetectionEndToEndIT.class);

  private static final String API_URL = System.getenv("API_URL");
  private static final String API_KEY = System.getenv("API_KEY");

  private static final Path OUTPUT_DIR =
      Path.of(envOrDefault("DETECTION_OUTPUT_DIR", "build/detections"));

  final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
  final HttpClient.Builder httpClientBuilder = HttpClient.newBuilder();
  final ApiClient apiClient = apiClient();
  final JsonArtifactWriter artifactWriter = new JsonArtifactWriter(objectMapper);

  private ApiClient apiClient() {
    var client = new ApiClient(httpClientBuilder, objectMapper, API_URL);
    client.setRequestInterceptor(builder -> builder.header(API_KEY_HEADER, API_KEY));
    return client;
  }

  @Test
  void detection_sync_it() throws IOException {
    assertNotNull(API_URL, "API_URL env var is required");
    assertNotNull(API_KEY, "API_KEY env var is required");

    var detectionApi = new DetectionApi(apiClient);
    var point =
        new AddressPoint(
            "1 Rue Victor Prouvé, 54110 Dombasle-sur-Meurthe, France", 48.609702, 6.351961);

    processDetectionSynchronously(point, detectionApi);
  }

  @Test
  void detection_2d_ko_csv_it() throws IOException {
    assertNotNull(API_URL, "API_URL env var is required");
    assertNotNull(API_KEY, "API_KEY env var is required");

    var detectionApi = new DetectionApi(apiClient);
    var points = AddressPointCsvReader.readResource(DETECTION_2D_KO_CSV);
    log.info("Processing {} detection(s) read from {}", points.size(), DETECTION_2D_KO_CSV);

    for (var point : points) {
      processDetectionSynchronously(point, detectionApi);
    }
  }

  private void processDetectionSynchronously(AddressPoint point, DetectionApi detectionApi)
      throws IOException {
    var zoneName = "debug archismart " + point.address();
    var detectionE2Id = randomUUID().toString();
    try {
      var detection =
          detectionApi.processDetectionSynchronously(
              detectionE2Id,
              new CreateDetectionDebugMode()
                  .emailReceiver("tech@birdia.fr")
                  .zoneName(zoneName)
                  .needsImageOutput(true)
                  .detectableObjectModelList(
                      List.of(new DetectableObjectModel().modelName(TOITURE)))
                  .geoJsonDelimitationType(PARCEL_FREE_DELIMITATION)
                  .geoJsonZone(List.of(toPointFeature(point)))
                  .geoJsonOutput(ZIP));
      assertNotNull(detection);
      dumpDetection(point, zoneName, detection);
    } catch (ApiException e) {
      assertNotNull(dumpFailedResponse(point, zoneName, e));
    }
  }

  private void dumpDetection(AddressPoint point, String zoneName, Detection detection)
      throws IOException {
    var target = artifactWriter.write(dumpPathOf(zoneName), detection, point);
    log.info("[{}] detection written to {}", zoneName, target.toAbsolutePath());
  }

  @SneakyThrows
  private Path dumpFailedResponse(AddressPoint point, String zoneName, ApiException e) {
    if (e.getCode() < 400) {
      log.error("[{}] detection call failed without any HTTP response", zoneName, e);
      return null;
    }
    var body = e.getResponseBody() == null ? "" : e.getResponseBody();

    var target = artifactWriter.writeBody(dumpPathOf("failed-" + zoneName), body, point);

    log.error(
        "[{}] detection failed with status {}, response written to {}",
        zoneName,
        e.getCode(),
        target.toAbsolutePath(),
        e);

    return target;
  }

  private static Path dumpPathOf(String name) {
    return OUTPUT_DIR.resolve(sanitize(name) + ".json");
  }

  private static String envOrDefault(String key, String defaultValue) {
    var value = System.getenv(key);
    return value == null || value.isBlank() ? defaultValue : value;
  }
}
