package app.bpartners.geojobs.endpoint.rest.security;

import static app.bpartners.geojobs.endpoint.rest.security.authenticator.ApiKeyAuthenticator.API_KEY_HEADER;
import static app.bpartners.geojobs.endpoint.rest.security.model.Authority.Role.ROLE_ADMIN;
import static java.net.http.HttpResponse.BodyHandlers.discarding;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.mockito.Mockito.when;

import app.bpartners.geojobs.conf.FacadeIT;
import app.bpartners.geojobs.endpoint.rest.controller.v1.CityJSONController;
import app.bpartners.geojobs.endpoint.rest.controller.v1.DetectionController;
import app.bpartners.geojobs.endpoint.rest.controller.v1.ImageController;
import app.bpartners.geojobs.repository.CommunityAuthorizationRepository;
import app.bpartners.geojobs.repository.model.community.CommunityAuthorization;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.server.LocalServerPort;

class V1PathAccessIT extends FacadeIT {
  private static final int FORBIDDEN = 403;
  private static final int OK = 200;
  private static final String ADMIN_API_KEY = "the-admin-api-key";

  @LocalServerPort private int port;
  @MockBean DetectionController detectionController;
  @MockBean CityJSONController cityJSONController;
  @MockBean ImageController imageController;
  @MockBean CommunityAuthorizationRepository caRepositoryMock;

  private static List<String> publicV1GetEndpoints() {
    return List.of(
        "/image",
        "/usage",
        "/detections",
        "/detections/dummy-id",
        "/city-jsons/dummy-id",
        "/3d/dummy-id");
  }

  @BeforeEach
  void setUp() {
    when(caRepositoryMock.findByApiKey(ADMIN_API_KEY))
        .thenReturn(
            Optional.of(
                CommunityAuthorization.builder().isApiKeyRevoked(false).role(ROLE_ADMIN).build()));
  }

  @Test
  void authenticated_admin_can_reach_public_endpoints_on_both_default_and_v1_paths() {
    for (var path : publicV1GetEndpoints()) {
      assertEquals(
          OK, statusOf(getWithApiKey(path)), () -> "default path should be accessible: " + path);
      assertEquals(
          OK,
          statusOf(getWithApiKey("/v1" + path)),
          () -> "v1 path should be accessible: /v1" + path);
    }
  }

  @Test
  void anonymous_is_denied_on_both_default_and_v1_paths() {
    for (var path : publicV1GetEndpoints()) {
      assertEquals(
          FORBIDDEN,
          statusOf(getAnonymously(path)),
          () -> "default path must require authentication: " + path);
      assertEquals(
          FORBIDDEN,
          statusOf(getAnonymously("/v1" + path)),
          () -> "v1 path must require authentication: /v1" + path);
    }
  }

  @Test
  void v1_paths_are_not_denied_by_default_deny_all() {
    for (var path : publicV1GetEndpoints()) {
      assertNotEquals(
          FORBIDDEN,
          statusOf(getWithApiKey("/v1" + path)),
          () -> "authenticated caller must not be forbidden on /v1" + path);
    }
  }

  private HttpResponse<Void> getWithApiKey(String path) {
    return send(HttpRequest.newBuilder(uri(path)).header(API_KEY_HEADER, ADMIN_API_KEY).GET());
  }

  private HttpResponse<Void> getAnonymously(String path) {
    return send(HttpRequest.newBuilder(uri(path)).GET());
  }

  private URI uri(String path) {
    return URI.create("http://localhost:" + port + path);
  }

  private static int statusOf(HttpResponse<Void> response) {
    return response.statusCode();
  }

  private HttpResponse<Void> send(HttpRequest.Builder builder) {
    try {
      return HttpClient.newHttpClient().send(builder.build(), discarding());
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
}
