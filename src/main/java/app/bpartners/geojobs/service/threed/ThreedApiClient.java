package app.bpartners.geojobs.service.threed;

import static org.springframework.web.util.UriComponentsBuilder.fromUri;

import app.bpartners.geojobs.service.threed.conf.ThreedApiProperties;
import app.bpartners.geojobs.service.threed.exception.ThreedApiException;
import app.bpartners.geojobs.service.threed.model.CreateLrgFromFeatureFileUrl;
import app.bpartners.geojobs.service.threed.model.Lrg;
import app.bpartners.geojobs.service.threed.model.Problem;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Component
public class ThreedApiClient {
  private final ObjectMapper objectMapper;
  private final RestTemplate restTemplate;
  private final ThreedApiProperties properties;
  private static final String PREFIX_PATH = "/lrg";
  private static final String SUFFIX_PATH = "/feature-file";
  private static final String API_KEY_QUERY_PARAM = "geojobs-apikey";

  public ThreedApiClient(
      @Qualifier("threedRestTemplate") RestTemplate restTemplate,
      ThreedApiProperties properties,
      ObjectMapper objectMapper) {
    this.restTemplate = restTemplate;
    this.properties = properties;
    this.objectMapper = objectMapper;
  }

  public Lrg generate(String id, CreateLrgFromFeatureFileUrl request) {
    return generate(id, request, null);
  }

  public Lrg generate(String id, CreateLrgFromFeatureFileUrl request, String apiKey) {
    var uri = buildGenerateUri(id, apiKey);

    var headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    headers.setAccept(java.util.List.of(MediaType.APPLICATION_JSON));

    var entity = new HttpEntity<>(request, headers);

    try {
      var response = restTemplate.exchange(uri, HttpMethod.PUT, entity, Lrg.class);
      return response.getBody();
    } catch (HttpStatusCodeException e) {
      throw mapHttpError(e);
    } catch (RestClientException e) {
      throw new ThreedApiException("Unable to call Threed API : " + e.getMessage(), e);
    }
  }

  @SneakyThrows
  private URI buildGenerateUri(String id, String apiKey) {
    var baseUri = new URI(properties.getBaseUrl());
    var builder = fromUri(baseUri).pathSegment(PREFIX_PATH, id, SUFFIX_PATH);

    if (apiKey != null) {
      builder = builder.queryParam(API_KEY_QUERY_PARAM, apiKey);
    }

    var uri = builder.build().toUri();
    log.info("Uri={}", uri);
    return uri;
  }

  private ThreedApiException mapHttpError(HttpStatusCodeException e) {
    String body = e.getResponseBodyAsString();
    String apiError = null;
    String apiErrorType = null;
    try {
      if (!body.isBlank()) {
        var parsed = objectMapper.readValue(body, Problem.class);
        apiError = parsed.getDetail();
        apiErrorType = parsed.getType();
      }
    } catch (Exception parseEx) {
      log.debug("Unable to parse response : {}", parseEx.getMessage());
    }

    var message =
        String.format(
            "Threed API error (HTTP %s) : %s",
            e.getStatusCode(), apiError != null ? apiError : body);
    return new ThreedApiException(e.getStatusCode(), apiErrorType, apiError, message);
  }
}
