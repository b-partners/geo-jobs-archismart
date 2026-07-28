package app.bpartners.geojobs.service.cacher.conf;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

@Slf4j
public class CacherApiClientConfig {
  public static RestTemplate getCacherRestTemplate(CacherApiProperties properties) {
    var factory = new SimpleClientHttpRequestFactory();
    factory.setConnectTimeout(properties.getConnectTimeoutMs());
    factory.setReadTimeout(properties.getReadTimeoutMs());

    return new RestTemplate(factory);
  }
}
