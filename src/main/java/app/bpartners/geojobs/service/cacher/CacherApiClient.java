package app.bpartners.geojobs.service.cacher;

import static app.bpartners.geojobs.service.cacher.conf.CacherApiClientConfig.getCacherRestTemplate;
import static org.springframework.web.util.UriComponentsBuilder.fromUriString;

import app.bpartners.geojobs.model.EncodedURL;
import app.bpartners.geojobs.service.cacher.conf.CacherApiProperties;
import app.bpartners.geojobs.service.cacher.exception.CacherApiException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;

@Slf4j
@Component
@RequiredArgsConstructor
@SuppressWarnings("all")
public class CacherApiClient {

  private final CacherApiProperties properties;

  private static final String PATH = "cached-url";
  private static final String ENCODED_URL_REQUEST_PARAM_NAME = "encodedUrl";
  private static final String API_KEY_REQUEST_PARAM_NAME = "apiKey";

  public URL getWithCache(URL decodedUrl) {
    var encodedUrl = new EncodedURL(decodedUrl);
    try {
      var uri = buildGetWithCacheUri(decodedUrl.toString(), properties);
      log.info("Cacher URI: {}, encodedUrl:{}", uri, encodedUrl.value());
      var restTemplate = getCacherRestTemplate(properties);
      var rawUrl = restTemplate.getForObject(uri, String.class);
      return new URL(rawUrl);
    } catch (RestClientException | MalformedURLException e) {
      throw new CacherApiException("Unable to call Cacher API : " + e.getMessage(), e);
    }
  }

  private URI buildGetWithCacheUri(String encodedUrl, CacherApiProperties properties) {
    return fromUriString(properties.getBaseUrl())
        .pathSegment(PATH)
        .queryParam(ENCODED_URL_REQUEST_PARAM_NAME, encodedUrl)
        .queryParam(API_KEY_REQUEST_PARAM_NAME, properties.getApiKey())
        .build()
        .toUri();
  }
}
