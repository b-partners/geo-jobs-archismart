package app.bpartners.geojobs.service.dashboard;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpMethod.PUT;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import app.bpartners.geojobs.service.dashboard.component.Register3d;
import java.net.URI;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

class AnalysisProjectApiTest {
  static final String BASE_URL = "https://dashboard.example.com";
  static final String API_KEY = "dashboardApiKey";
  static final String RESPONSE = "{\"projectKey\":\"key\",\"creditsSufficient\":true}";

  ApiConfiguration apiConfiguration = new ApiConfiguration(BASE_URL);
  SecurityApi securityApiMock = mock();
  RestTemplate restTemplate = AnalysisProjectApi.strictlyEncodingRestTemplate();
  MockRestServiceServer server = MockRestServiceServer.createServer(restTemplate);
  AnalysisProjectApi subject;

  @BeforeEach
  void setUp() {
    when(securityApiMock.retrieveUserId(API_KEY)).thenReturn("user-id");
    subject = new AnalysisProjectApi(apiConfiguration, securityApiMock, restTemplate);
  }

  @Test
  void get_a_project_with_the_dashboard_api_key() {
    expect(GET, "/users/user-id/analysisProjects/v1%3Aabc123");

    var actual = subject.get(API_KEY, "v1:abc123");

    assertEquals("key", actual.projectKey());
    server.verify();
  }

  @Test
  void register_3d_on_the_project_step() {
    expect(PUT, "/users/user-id/analysisProjects/city-json%3Arequest/3d");

    subject.register3d(API_KEY, "city-json:request", new Register3d(null));

    server.verify();
  }

  @Test
  void project_key_cannot_add_path_segments() {
    expect(PUT, "/users/user-id/analysisProjects/a%2F..%2F..%2Fother/3d");

    subject.register3d(API_KEY, "a/../../other", new Register3d(null));

    server.verify();
  }

  @Test
  void project_key_cannot_alter_the_query() {
    expect(GET, "/users/user-id/analysisProjects/key%3Fadmin%3Dtrue%23frag");

    subject.get(API_KEY, "key?admin=true#frag");

    server.verify();
  }

  @Test
  void project_key_cannot_be_a_relative_path_segment() {
    assertThrows(IllegalArgumentException.class, () -> subject.get(API_KEY, ".."));
    assertThrows(IllegalArgumentException.class, () -> subject.get(API_KEY, "."));
  }

  @Test
  void project_key_cannot_be_blank() {
    assertThrows(IllegalArgumentException.class, () -> subject.get(API_KEY, " "));
    assertThrows(IllegalArgumentException.class, () -> subject.get(API_KEY, null));
  }

  private void expect(HttpMethod httpMethod, String path) {
    server
        .expect(requestTo(URI.create(BASE_URL + path)))
        .andExpect(method(httpMethod))
        .andExpect(header("x-api-key", API_KEY))
        .andRespond(withSuccess(RESPONSE, APPLICATION_JSON));
  }
}
