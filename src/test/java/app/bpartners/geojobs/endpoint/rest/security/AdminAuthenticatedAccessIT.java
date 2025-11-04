package app.bpartners.geojobs.endpoint.rest.security;

import static app.bpartners.geojobs.endpoint.rest.security.authenticator.ApiKeyAuthenticator.API_KEY_HEADER;
import static app.bpartners.geojobs.endpoint.rest.security.model.Authority.Role.ROLE_ADMIN;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import app.bpartners.geojobs.conf.FacadeIT;
import app.bpartners.geojobs.endpoint.rest.api.DetectionApi;
import app.bpartners.geojobs.endpoint.rest.api.MachineDetectionApi;
import app.bpartners.geojobs.endpoint.rest.api.TilingApi;
import app.bpartners.geojobs.endpoint.rest.client.ApiClient;
import app.bpartners.geojobs.endpoint.rest.client.ApiException;
import app.bpartners.geojobs.endpoint.rest.controller.ZoneDetectionController;
import app.bpartners.geojobs.endpoint.rest.controller.ZoneTilingController;
import app.bpartners.geojobs.endpoint.rest.model.CreateZoneTilingJob;
import app.bpartners.geojobs.endpoint.rest.model.ZoneDetectionJob;
import app.bpartners.geojobs.endpoint.rest.model.ZoneTilingJob;
import app.bpartners.geojobs.repository.CommunityAuthorizationRepository;
import app.bpartners.geojobs.repository.model.community.CommunityAuthorization;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.server.LocalServerPort;

public class AdminAuthenticatedAccessIT extends FacadeIT {

  private static final String ADMIN_API_KEY = "the-admin-api-key";
  @LocalServerPort private int port;

  @Autowired ObjectMapper om;

  TilingApi tilingApi;
  DetectionApi detectionApi;
  MachineDetectionApi machineDetectionApi;

  @MockBean CommunityAuthorizationRepository caRepositoryMock;
  @MockBean ZoneTilingController tilingController;
  @MockBean ZoneDetectionController detectionController;

  @BeforeEach
  void setUp() {
    var authenticatedClient = new ApiClient();
    authenticatedClient.setRequestInterceptor(
        builder -> builder.header(API_KEY_HEADER, ADMIN_API_KEY));
    authenticatedClient.setScheme("http");
    authenticatedClient.setHost("localhost");
    authenticatedClient.setPort(port);
    authenticatedClient.setObjectMapper(om);

    tilingApi = new TilingApi(authenticatedClient);
    detectionApi = new DetectionApi(authenticatedClient);
    machineDetectionApi = new MachineDetectionApi(authenticatedClient);
    when(caRepositoryMock.findByApiKey(ADMIN_API_KEY))
        .thenReturn(
            Optional.of(
                CommunityAuthorization.builder().isApiKeyRevoked(false).role(ROLE_ADMIN).build()));
  }

  @Test
  void admin_can_tile() throws ApiException {
    var expected = new ZoneTilingJob();
    when(tilingController.tileZone(any())).thenReturn(expected);

    var actual = tilingApi.tileZone(mock(CreateZoneTilingJob.class));

    assertEquals(expected, actual);
  }

  @Test
  void admin_can_detect() throws ApiException {
    var expected = new ZoneDetectionJob();
    when(detectionController.processZDJ(any(), any())).thenReturn(expected);

    var actual = machineDetectionApi.processZDJ("dummy", List.of());

    assertEquals(expected, actual);
  }
}
