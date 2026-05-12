package app.bpartners.geojobs.service.roofer3dbag.conf;

import java.util.Collections;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

/**
 * Configuration du RestTemplate utilisé par le client roofer.
 *
 * <p>- Injecte l'en-tête x-api-key sur toutes les requêtes via un intercepteur. - Configure les
 * timeouts (la génération CityJSON peut prendre plusieurs minutes).
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
@EnableConfigurationProperties
public class ApiClientConfig {

  private static final String API_KEY_HEADER = "x-api-key";

  private final RooferApiProperties properties;

  @Bean("rooferRestTemplate")
  public RestTemplate rooferRestTemplate() {
    if (properties.getApiKey() == null || properties.getApiKey().isBlank()) {
      log.warn("Unable to retrieve ROOFER_API_KEY");
    }

    SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
    factory.setConnectTimeout(properties.getConnectTimeoutMs());
    factory.setReadTimeout(properties.getReadTimeoutMs());

    RestTemplate restTemplate = new RestTemplate(factory);
    restTemplate.setInterceptors(Collections.singletonList(apiKeyInterceptor()));
    return restTemplate;
  }

  private ClientHttpRequestInterceptor apiKeyInterceptor() {
    return (request, body, execution) -> {
      String key = properties.getApiKey();
      if (key != null && !key.isBlank()) {
        request.getHeaders().set(API_KEY_HEADER, key);
      }
      return execution.execute(request, body);
    };
  }
}
