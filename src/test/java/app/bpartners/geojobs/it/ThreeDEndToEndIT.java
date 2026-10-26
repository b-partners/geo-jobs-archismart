package app.bpartners.geojobs.it;

import static app.bpartners.geojobs.endpoint.rest.model.DelimitationObjectType.BUILDING_ROOF;
import static app.bpartners.geojobs.endpoint.rest.model.DelimitationType.PARCEL_FREE_DELIMITATION;
import static app.bpartners.geojobs.endpoint.rest.model.Status.HealthEnum.FAILED;
import static app.bpartners.geojobs.endpoint.rest.model.Status.ProgressionEnum.FINISHED;
import static app.bpartners.geojobs.endpoint.rest.security.authenticator.ApiKeyAuthenticator.API_KEY_HEADER;
import static app.bpartners.geojobs.utils.it.AddressPointCsvReader.DETECTION_2D_KO_CSV;
import static app.bpartners.geojobs.utils.it.AddressPointFeatures.toPointFeature;
import static app.bpartners.geojobs.utils.it.JsonArtifactWriter.sanitize;
import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import app.bpartners.geojobs.endpoint.rest.api.ThreeDApi;
import app.bpartners.geojobs.endpoint.rest.client.ApiClient;
import app.bpartners.geojobs.endpoint.rest.client.ApiException;
import app.bpartners.geojobs.endpoint.rest.model.*;
import app.bpartners.geojobs.utils.it.AddressPoint;
import app.bpartners.geojobs.utils.it.AddressPointCsvReader;
import app.bpartners.geojobs.utils.it.JsonArtifactWriter;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 3D counterpart of {@link DetectionEndToEndIT}, taking the very same inputs ({@link AddressPoint}
 * : an {@code address} and its {@code coordinates}) but calling the <b>asynchronous</b> {@code POST
 * /3d/{id}} instead of the synchronous detection endpoint : the request only returns an accepted
 * status, so each one is then polled through {@code GET /3d/{id}} until its cityJSON files are
 * available.
 *
 * <p>Every request writes, under {@code build/three-d/<address>/} :
 *
 * <ul>
 *   <li>{@code <address>.json} : the last {@code ThreeDResponseStatus}, enriched with the {@code
 *       address} and {@code coordinates} attributes of the submitted request,
 *   <li>{@code failed-<address>.json} : the 4xx/5xx body, same enrichment,
 *   <li>{@code <address>-<n>.city.json} : the cityJSON files, downloaded as returned by the API
 *       (their pre-signed URLs expire one hour after generation).
 * </ul>
 */
@Disabled("Local use only")
class ThreeDEndToEndIT {
  private static final Logger log = LoggerFactory.getLogger(ThreeDEndToEndIT.class);

  private static final String API_URL = System.getenv("API_URL");
  private static final String API_KEY = System.getenv("API_KEY");

  private static final Path OUTPUT_DIR =
      Path.of(envOrDefault("THREE_D_OUTPUT_DIR", "build/three-d"));
  private static final Duration POLL_TIMEOUT =
      Duration.ofMinutes(Long.parseLong(envOrDefault("THREE_D_POLL_TIMEOUT_MINUTES", "20")));
  private static final Duration POLL_INTERVAL =
      Duration.ofSeconds(Long.parseLong(envOrDefault("THREE_D_POLL_INTERVAL_SECONDS", "30")));

  final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
  final HttpClient.Builder httpClientBuilder = HttpClient.newBuilder();
  final ApiClient apiClient = apiClient();
  final HttpClient downloadClient = HttpClient.newHttpClient();
  final JsonArtifactWriter artifactWriter = new JsonArtifactWriter(objectMapper);

  private ApiClient apiClient() {
    var client = new ApiClient(httpClientBuilder, objectMapper, API_URL);
    client.setRequestInterceptor(builder -> builder.header(API_KEY_HEADER, API_KEY));
    return client;
  }

  @Test
  void three_d_async_it() {
    assertNotNull(API_URL, "API_URL env var is required");
    assertNotNull(API_KEY, "API_KEY env var is required");

    var threeDApi = new ThreeDApi(apiClient);
    var point =
        new AddressPoint(
            "1 Rue Victor Prouvé, 54110 Dombasle-sur-Meurthe, France", 48.609702, 6.351961);

    process3DRequest(point, threeDApi);
  }

  /**
   * Replays every row of {@code detection_2d_ko.csv} as a 3D request. Rows are submitted and polled
   * concurrently, one virtual thread each, since a single request may take several minutes ; each
   * one writes its own artifacts, so a failing address does not stop the others.
   */
  @Test
  void three_d_2d_ko_csv_it() throws IOException, InterruptedException {
    assertNotNull(API_URL, "API_URL env var is required");
    assertNotNull(API_KEY, "API_KEY env var is required");

    var threeDApi = new ThreeDApi(apiClient);
    var points = AddressPointCsvReader.readResource(DETECTION_2D_KO_CSV);
    log.info("Processing {} 3D request(s) read from {}", points.size(), DETECTION_2D_KO_CSV);

    List<Callable<Void>> callables =
        points.stream()
            .map(
                point ->
                    (Callable<Void>)
                        () -> {
                          process3DRequest(point, threeDApi);
                          return null;
                        })
            .toList();
    List<Future<Void>> futures;
    try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
      futures = executor.invokeAll(callables);
    }

    // 4xx/5xx are already dumped by each callable ; anything left here is a failure invokeAll
    // swallowed into its Future, which would otherwise go unnoticed
    var failed = 0;
    for (int i = 0; i < futures.size(); i++) {
      try {
        futures.get(i).get();
      } catch (ExecutionException e) {
        failed++;
        log.error("[{}] 3D request could not be processed", points.get(i).address(), e.getCause());
      }
    }
    log.info(
        "Done, {}/{} request(s) could not be processed. Results under {}",
        failed,
        points.size(),
        OUTPUT_DIR.toAbsolutePath());
  }

  /**
   * Submits one 3D request on the given point, polls it until completion, then writes its status
   * and downloads the produced cityJSON files.
   */
  private void process3DRequest(AddressPoint point, ThreeDApi threeDApi) {
    var requestId = randomUUID().toString();
    log.info("[{}] submitting 3D request (id={})", point.address(), requestId);

    try {
      var accepted =
          threeDApi.request3DFileOnDelimitations(
              requestId,
              new ThreeDRequest()
                  .delimitationType(PARCEL_FREE_DELIMITATION)
                  .delimitationObjectType(BUILDING_ROOF)
                  .delimitations(List.of(toPointFeature(point))));
      assertNotNull(accepted);

      var status = pollUntilCompleted(threeDApi, requestId, point);
      dumpStatus(point, status);
      downloadCityJsonFiles(point, status);
    } catch (ApiException e) {
      assertNotNull(dumpFailedResponse(point, e));
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException("Interrupted while polling 3D request " + requestId, e);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  /**
   * Polls {@code GET /3d/{id}} every {@link #POLL_INTERVAL} until the request is finished, failed,
   * or {@link #POLL_TIMEOUT} elapses, and returns the last status read. A finished request is one
   * whose progression is {@code FINISHED} and which exposes at least one cityJSON file URL.
   */
  private ThreeDResponseStatus pollUntilCompleted(
      ThreeDApi threeDApi, String requestId, AddressPoint point)
      throws ApiException, InterruptedException {
    var deadline = System.nanoTime() + POLL_TIMEOUT.toNanos();
    ThreeDResponseStatus status = threeDApi.getRequested3DFileById(requestId);
    while (true) {
      if (isCompleted(status)) {
        log.info("[{}] 3D request {} completed", point.address(), requestId);
        return status;
      }
      if (isFailed(status)) {
        log.error("[{}] 3D request {} failed: {}", point.address(), requestId, describe(status));
        return status;
      }
      var remaining = deadline - System.nanoTime();
      if (remaining <= 0) {
        log.warn(
            "[{}] 3D request {} still {} after {}",
            point.address(),
            requestId,
            describe(status),
            POLL_TIMEOUT);
        return status;
      }
      TimeUnit.NANOSECONDS.sleep(Math.min(POLL_INTERVAL.toNanos(), remaining));
      status = threeDApi.getRequested3DFileById(requestId);
    }
  }

  private static boolean isCompleted(ThreeDResponseStatus status) {
    return status.getStatus() != null
        && status.getStatus().getProgression() == FINISHED
        && status.getCityJsonFileUrls() != null
        && !status.getCityJsonFileUrls().isEmpty();
  }

  private static boolean isFailed(ThreeDResponseStatus status) {
    return status.getStatus() != null && status.getStatus().getHealth() == FAILED;
  }

  /** Human-readable {@code step progression/health} of a status, for logs. */
  private static String describe(ThreeDResponseStatus status) {
    var currentStatus = status.getStatus();
    return status.getStep()
        + " "
        + (currentStatus == null
            ? "without status"
            : currentStatus.getProgression() + "/" + currentStatus.getHealth());
  }

  /** Downloads every cityJSON file the completed request exposes, keeping the files untouched. */
  private void downloadCityJsonFiles(AddressPoint point, ThreeDResponseStatus status)
      throws IOException, InterruptedException {
    var urls = status.getCityJsonFileUrls();
    if (urls == null || urls.isEmpty()) {
      log.warn("[{}] no cityJson file to download", point.address());
      return;
    }
    for (int i = 0; i < urls.size(); i++) {
      var url = urls.get(i).getUrl();
      if (url == null) {
        continue;
      }
      var target = outputDirOf(point).resolve(sanitize(point.address()) + "-" + i + ".city.json");
      download(url, target);
      log.info("[{}] cityJson written to {}", point.address(), target.toAbsolutePath());
    }
  }

  private void download(String url, Path target) throws IOException, InterruptedException {
    var request = HttpRequest.newBuilder(URI.create(url)).GET().build();
    var response = downloadClient.send(request, HttpResponse.BodyHandlers.ofInputStream());
    if (response.statusCode() / 100 != 2) {
      throw new IOException("Download failed (" + response.statusCode() + ") for " + url);
    }
    Files.createDirectories(target.getParent());
    try (var in = response.body()) {
      Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
    }
  }

  /** Writes the last polled status into {@code <OUTPUT_DIR>/<address>/<address>.json}. */
  private void dumpStatus(AddressPoint point, ThreeDResponseStatus status) throws IOException {
    var target = artifactWriter.write(dumpPathOf(point, sanitize(point.address())), status, point);
    log.info("[{}] 3D status written to {}", point.address(), target.toAbsolutePath());
  }

  /**
   * On a 4xx/5xx response, writes its body into {@code
   * <OUTPUT_DIR>/<address>/failed-<address>.json} and returns that path. Non-HTTP failures
   * (connection errors, where the generated client wraps the cause and leaves the code at 0) have
   * no body to dump : they only get logged and yield {@code null}.
   */
  private Path dumpFailedResponse(AddressPoint point, ApiException e) {
    if (e.getCode() < 400) {
      log.error("[{}] 3D request failed without any HTTP response", point.address(), e);
      return null;
    }
    var body = e.getResponseBody() == null ? "" : e.getResponseBody();
    Path target;
    try {
      target =
          artifactWriter.writeBody(
              dumpPathOf(point, "failed-" + sanitize(point.address())), body, point);
    } catch (IOException ioException) {
      throw new UncheckedIOException(ioException);
    }
    log.error(
        "[{}] 3D request failed with status {}, response written to {}",
        point.address(),
        e.getCode(),
        target.toAbsolutePath(),
        e);
    return target;
  }

  private static Path dumpPathOf(AddressPoint point, String name) {
    return outputDirOf(point).resolve(name + ".json");
  }

  /** One folder per address, holding its status dump and its cityJSON files. */
  private static Path outputDirOf(AddressPoint point) {
    return OUTPUT_DIR.resolve(sanitize(point.address()));
  }

  private static String envOrDefault(String key, String defaultValue) {
    var value = System.getenv(key);
    return value == null || value.isBlank() ? defaultValue : value;
  }
}
