package app.bpartners.geojobs.service.geodata;

import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpMethod.POST;

import app.bpartners.geojobs.model.exception.GatewayTimeoutException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.util.Map;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Component
@Slf4j
public class GeodataHttpClient {
  private static final String API_KEY_HEADER = "x-api-key";
  private static final String RESOURCE_NOT_FOUND_TYPE = "ResourceNotFoundException";

  private final ObjectMapper om;
  private final RestTemplate restTemplate;
  private final GeodataApiConf conf;

  public GeodataHttpClient(
      ObjectMapper om,
      @Qualifier(GeodataRestTemplateConf.GEODATA_REST_TEMPLATE) RestTemplate restTemplate,
      GeodataApiConf conf) {
    this.om = om;
    this.restTemplate = restTemplate;
    this.conf = conf;
  }

  public <T> T post(String path, Object body, Class<T> responseType) {
    var headers = headers();
    headers.add(HttpHeaders.CONTENT_TYPE, "application/json");
    return exchange(uri(path, Map.of()), POST, new HttpEntity<>(body, headers), responseType, false)
        .orElseThrow(() -> unavailable(path, "empty response body"));
  }

  public <T> T get(String path, Map<String, Object> queryParams, Class<T> responseType) {
    return getOrEmptyWhenNotFound(path, queryParams, responseType)
        .orElseThrow(() -> unavailable(path, "resource not found"));
  }

  public <T> Optional<T> getOrEmptyWhenNotFound(
      String path, Map<String, Object> queryParams, Class<T> responseType) {
    return exchange(uri(path, queryParams), GET, new HttpEntity<>(headers()), responseType, true);
  }

  private <T> Optional<T> exchange(
      URI uri,
      HttpMethod method,
      HttpEntity<?> request,
      Class<T> responseType,
      boolean notFoundIsEmpty) {
    try {
      var response = restTemplate.exchange(uri, method, request, responseType);
      if (response.getStatusCode() != HttpStatus.OK) {
        throw unavailable(uri.getPath(), "HTTP code " + response.getStatusCode());
      }
      return Optional.ofNullable(response.getBody());
    } catch (HttpClientErrorException.NotFound e) {
      if (notFoundIsEmpty && isResourceNotFound(e)) {
        return Optional.empty();
      }
      throw unavailable(uri.getPath(), e.getMessage());
    } catch (RestClientException e) {
      log.warn("Geodata API call {} {} failed", method, uri.getPath(), e);
      throw unavailable(uri.getPath(), e.getMessage());
    }
  }

  private boolean isResourceNotFound(HttpClientErrorException.NotFound e) {
    try {
      var type = om.readTree(e.getResponseBodyAsString()).path("type").asText();
      return RESOURCE_NOT_FOUND_TYPE.equals(type);
    } catch (JsonProcessingException | RuntimeException parsingFailure) {
      return false;
    }
  }

  private HttpHeaders headers() {
    var headers = new HttpHeaders();
    headers.add(API_KEY_HEADER, conf.getKey());
    return headers;
  }

  private URI uri(String path, Map<String, Object> queryParams) {
    var builder = UriComponentsBuilder.fromHttpUrl(conf.getUrl() + path);
    queryParams.forEach(builder::queryParam);
    return builder.build().encode().toUri();
  }

  private GatewayTimeoutException unavailable(String path, String reason) {
    return new GatewayTimeoutException(
        "Geodata API at " + conf.getUrl() + path + " is unavailable: " + reason);
  }
}
