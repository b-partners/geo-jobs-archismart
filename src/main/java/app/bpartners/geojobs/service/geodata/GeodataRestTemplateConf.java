package app.bpartners.geojobs.service.geodata;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

@Configuration
public class GeodataRestTemplateConf {
  static final String GEODATA_REST_TEMPLATE = "geodataRestTemplate";
  static final int CONNECT_TIMEOUT_MS = 10_000;
  static final int READ_TIMEOUT_MS = 60_000;

  @Bean(GEODATA_REST_TEMPLATE)
  public RestTemplate geodataRestTemplate() {
    var factory = new SimpleClientHttpRequestFactory();
    factory.setConnectTimeout(CONNECT_TIMEOUT_MS);
    factory.setReadTimeout(READ_TIMEOUT_MS);
    return new RestTemplate(factory);
  }
}
