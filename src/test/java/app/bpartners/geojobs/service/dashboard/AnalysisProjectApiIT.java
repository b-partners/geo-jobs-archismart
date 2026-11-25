package app.bpartners.geojobs.service.dashboard;

import static java.time.Instant.now;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import app.bpartners.geojobs.endpoint.rest.security.model.Principal;
import app.bpartners.geojobs.service.dashboard.component.DetectionInitiator;
import app.bpartners.geojobs.service.dashboard.component.Register2d;
import app.bpartners.geojobs.service.dashboard.component.Register3d;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestTemplate;

@Disabled("TODO: local use only, disable otherwise")
@Slf4j
class AnalysisProjectApiIT {
  private final String apiKey = System.getenv("API_KEY");
  final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
  final ApiConfiguration apiConfiguration =
      new ApiConfiguration(System.getenv("BPARTNERS_API_URL"));
  final RestTemplate restTemplate = new RestTemplate();
  AnalysisProjectApi subject =
      new AnalysisProjectApi(
          apiConfiguration, new SecurityApi(restTemplate, apiConfiguration, objectMapper));

  @BeforeEach
  void setUp() {
    var principalMock = mock(Principal.class);
    when(principalMock.getApiKey()).thenReturn(apiKey);
  }

  @Test
  void register_2d_then_3d_then_get() {
    var projectKey = "v1:" + System.currentTimeMillis();

    var after2d =
        subject.register2d(
            apiKey,
            projectKey,
            new Register2d(
                "detection end to end id from geo-jobs",
                "zone from geo-jobs",
                "address from geo-jobs",
                now(),
                new DetectionInitiator("Ryan", "ryan@email.com", "0611223344")));
    var after3d = subject.register3d(apiKey, projectKey, new Register3d("address from geo-jobs"));
    var actual = subject.get(apiKey, projectKey);

    assertNotNull(after2d);
    assertNotNull(after3d);
    assertNotNull(actual);
    log.info(actual.toString());
  }
}
