package app.bpartners.geojobs.service.ciytjsonprocessor.conf;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Configuration
@RequiredArgsConstructor
@EnableConfigurationProperties
public class CityJsonProcessorApiClientConfig {
  private final CityJsonProcessorApiProperties properties;

  @Bean("cityJsonProcessorRestTemplate")
  public RestTemplate rooferRestTemplate() {
    var factory = new SimpleClientHttpRequestFactory();
    factory.setConnectTimeout(properties.getConnectTimeoutMs());
    factory.setReadTimeout(properties.getReadTimeoutMs());

    return new RestTemplate(factory);
  }
}
