package app.bpartners.geojobs.service.coverage;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

@Configuration
public class LidarFileRestTemplateConf {
  static final String LIDAR_FILE_REST_TEMPLATE = "lidarFileRestTemplate";
  static final int CONNECT_TIMEOUT_MS = 10_000;
  static final int READ_TIMEOUT_MS = 10_000;

  @Bean(LIDAR_FILE_REST_TEMPLATE)
  public RestTemplate lidarFileRestTemplate() {
    var factory = new SimpleClientHttpRequestFactory();
    factory.setConnectTimeout(CONNECT_TIMEOUT_MS);
    factory.setReadTimeout(READ_TIMEOUT_MS);
    return new RestTemplate(factory);
  }
}
