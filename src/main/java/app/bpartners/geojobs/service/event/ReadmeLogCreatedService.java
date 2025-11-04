package app.bpartners.geojobs.service.event;

import static java.net.http.HttpClient.Version;
import static java.net.http.HttpClient.newHttpClient;
import static java.net.http.HttpRequest.BodyPublishers;
import static java.net.http.HttpResponse.BodyHandlers;

import app.bpartners.geojobs.endpoint.event.model.readme.ReadmeLogCreated;
import app.bpartners.geojobs.endpoint.rest.readme.monitor.ReadmeMonitorConf;
import app.bpartners.geojobs.endpoint.rest.readme.monitor.model.ReadmeLog;
import app.bpartners.geojobs.model.exception.BadRequestException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Base64;
import java.util.List;
import java.util.function.Consumer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReadmeLogCreatedService implements Consumer<ReadmeLogCreated> {
  private static final String README_AUTH_PREFIX = "Basic ";
  private static final String README_API_MIME_TYPE = "application/json";
  private final ObjectMapper objectMapper;
  private static final HttpClient httpClient = newHttpClient();

  @Override
  public void accept(ReadmeLogCreated readmeLogCreated) {
    var readmeMonitorConf = readmeLogCreated.getReadmeMonitorConf();
    var readmeLog = readmeLogCreated.getReadmeLog();

    if (readmeMonitorConf.isDevelopment() != readmeLog.development()) {
      throw new BadRequestException(
          "readmeLog.development should be " + readmeMonitorConf.isDevelopment());
    }

    try {
      saveReadmeLog(readmeLog, readmeMonitorConf);
    } catch (IOException e) {
      log.error(e.getMessage());
      throw new RuntimeException("Cannot save readmeLog");
    } catch (InterruptedException e) {
      log.error(e.getMessage());
      Thread.currentThread().interrupt();
      throw new RuntimeException("Cannot save readmeLog");
    }
  }

  private String getBasicAuthValue(ReadmeMonitorConf readmeMonitorConf) {
    String authInfo = readmeMonitorConf.getApiKey() + ":";
    return README_AUTH_PREFIX + Base64.getEncoder().encodeToString(authInfo.getBytes());
  }

  private void saveReadmeLog(ReadmeLog readmeLog, ReadmeMonitorConf readmeMonitorConf)
      throws IOException, InterruptedException {
    String requestBody = objectMapper.writeValueAsString(List.of(readmeLog));
    HttpRequest httpRequest =
        HttpRequest.newBuilder()
            .uri(URI.create(readmeMonitorConf.getUrl()))
            .header("Content-Type", README_API_MIME_TYPE)
            .header("Authorization", getBasicAuthValue(readmeMonitorConf))
            .POST(BodyPublishers.ofString(requestBody))
            .build();
    HttpResponse<String> readmeResponse = httpClient.send(httpRequest, BodyHandlers.ofString());

    log.info("readme.monitor.requestBody: {}", requestBody);
    log.info("readme.monitor.responseBody: {}", readmeResponse.body());
    log.info("readme.monitor.responseStatus : {}", readmeResponse.statusCode());
  }

  public static Version getClientVersion() {
    return httpClient.version();
  }
}
