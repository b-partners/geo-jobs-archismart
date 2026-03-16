package app.bpartners.geojobs.service.dashboard;

import static app.bpartners.geojobs.service.dashboard.DashboardUserStatus.ACTIVE;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestTemplate;

@Disabled("Local use only")
class SecurityApiIT {
  SecurityApi subject =
      new SecurityApi(
          new RestTemplate(),
          new ApiConfiguration(System.getenv("API_URL")),
          new ObjectMapper().findAndRegisterModules());

  @Test
  void get_user_by_api_key() {
    var actual = subject.retrieveDashboardUserByApiKey(System.getenv("API_KEY"));

    assertEquals(ACTIVE, actual.subscription().status());
  }
}
