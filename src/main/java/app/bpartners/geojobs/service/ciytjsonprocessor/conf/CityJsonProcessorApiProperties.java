package app.bpartners.geojobs.service.ciytjsonprocessor.conf;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Getter
@Component
public class CityJsonProcessorApiProperties {
  private static final int CONNECT_TIMEOUT_MS = 50_000;
  private static final int READ_TIMEOUT_MS = 300_000;

  private final String baseUrl;
  private final int readTimeoutMs;
  private final int connectTimeoutMs;

  public CityJsonProcessorApiProperties(@Value("${city-json-processor.api.url}") String baseUrl) {
    this.baseUrl = baseUrl;
    this.readTimeoutMs = READ_TIMEOUT_MS;
    this.connectTimeoutMs = CONNECT_TIMEOUT_MS;
  }
}
