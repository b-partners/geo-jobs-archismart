package app.bpartners.geojobs.endpoint.rest.security;

import static java.net.http.HttpClient.newHttpClient;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import app.bpartners.geojobs.conf.FacadeIT;
import app.bpartners.geojobs.endpoint.rest.api.DetectionApi;
import app.bpartners.geojobs.endpoint.rest.api.MachineDetectionApi;
import app.bpartners.geojobs.endpoint.rest.api.TilingApi;
import app.bpartners.geojobs.endpoint.rest.client.ApiClient;
import app.bpartners.geojobs.endpoint.rest.client.ApiException;
import app.bpartners.geojobs.endpoint.rest.model.CreateZoneTilingJob;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse.BodyHandlers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.server.LocalServerPort;

public class AnonymousAccessIT extends FacadeIT {

  @LocalServerPort private int port;

  @Autowired ObjectMapper om;

  TilingApi tilingApi;
  DetectionApi detectionApi;
  MachineDetectionApi machineDetectionApi;

  @BeforeEach
  void setUp() {
    var anonymousClient = new ApiClient();
    anonymousClient.setScheme("http");
    anonymousClient.setPort(port);
    anonymousClient.setHost("localhost");
    anonymousClient.setObjectMapper(om);

    tilingApi = new TilingApi(anonymousClient);
    detectionApi = new DetectionApi(anonymousClient);
    machineDetectionApi = new MachineDetectionApi(anonymousClient);
  }

  @Test
  void anonymous_can_ping() throws URISyntaxException, IOException, InterruptedException {
    var pongResponse =
        newHttpClient()
            .send(
                HttpRequest.newBuilder()
                    .GET()
                    .uri(new URI("http://localhost:" + port + "/ping"))
                    .build(),
                BodyHandlers.ofString());
    assertEquals("pong", pongResponse.body());
  }

  @Test
  void anonymous_cannot_tile() {
    var e = assertThrows(ApiException.class, () -> tilingApi.getTilingJobs(1, 2));
    assertTrue(e.getMessage().contains("Bad credentials"));

    e = assertThrows(ApiException.class, () -> tilingApi.getZTJParcels("dummy"));
    assertTrue(e.getMessage().contains("Bad credentials"));

    e = assertThrows(ApiException.class, () -> tilingApi.tileZone(mock(CreateZoneTilingJob.class)));
    assertTrue(e.getMessage().contains("Bad credentials"));
  }

  @Test
  void anonymous_cannot_detect() {
    var e = assertThrows(ApiException.class, () -> machineDetectionApi.getDetectionJobs(1, 2));
    assertTrue(e.getMessage().contains("Bad credentials"));

    e = assertThrows(ApiException.class, () -> machineDetectionApi.getZDJGeojsonsUrl("dummy"));
    assertTrue(e.getMessage().contains("Bad credentials"));
  }
}
