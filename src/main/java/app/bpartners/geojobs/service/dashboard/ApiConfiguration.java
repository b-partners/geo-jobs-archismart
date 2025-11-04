package app.bpartners.geojobs.service.dashboard;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
@Getter
public class ApiConfiguration {
  static final String API_KEY_HEADER = "x-api-key";
  private final String dashboardApiUrl;

  public ApiConfiguration(@Value("${bpartners.api.url}") String dashboardApiUrl) {
    this.dashboardApiUrl = dashboardApiUrl;
  }
}
