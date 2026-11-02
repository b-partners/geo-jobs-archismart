package app.bpartners.geojobs.service.threed.conf;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Getter
@Component
public class ThreedApiProperties {
  private static final int CONNECT_TIMEOUT_MS = 50_000;
  private static final int READ_TIMEOUT_MS = 300_000;

  private final String baseUrl;
  private final String apiKey;
  private final int readTimeoutMs;
  private final int connectTimeoutMs;

  public ThreedApiProperties(
      @Value("${threed.api.url}") String baseUrl, @Value("${threed.api.key}") String apiKey) {
    this.baseUrl = baseUrl;
    this.apiKey = apiKey;
    this.readTimeoutMs = READ_TIMEOUT_MS;
    this.connectTimeoutMs = CONNECT_TIMEOUT_MS;
  }
}
