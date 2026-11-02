package app.bpartners.geojobs.service.threed;

import static org.springframework.web.util.UriComponentsBuilder.fromUri;

import app.bpartners.geojobs.endpoint.rest.model.CreateRoofIntersections;
import app.bpartners.geojobs.endpoint.rest.model.RoofIntersections;
import app.bpartners.geojobs.model.exception.BadRequestException;
import app.bpartners.geojobs.service.threed.conf.ThreedApiProperties;
import app.bpartners.geojobs.service.threed.exception.ThreedApiException;
import app.bpartners.geojobs.service.threed.model.CreateLrgFromFeatureFileUrl;
import app.bpartners.geojobs.service.threed.model.Lrg;
import app.bpartners.geojobs.service.threed.model.Problem;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
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
import org.springframework.web.util.UriComponentsBuilder;

@Slf4j
@Component
public class ThreedApiClient {
  private final ObjectMapper objectMapper;
  private final RestTemplate restTemplate;
  private final ThreedApiProperties properties;
  private static final String PREFIX_PATH = "lrg";
  private static final String SUFFIX_PATH = "feature-file";
  private static final String INTERSECTIONS_PATH = "/intersections/";
  private static final Pattern SAFE_ID = Pattern.compile("[A-Za-z0-9_-]{1,128}");
  private static final String API_KEY_QUERY_PARAM = "geojobs-apikey";
  private static final String THREED_API_KEY_QUERY_PARAM = "apiKey";

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
    return put(buildGenerateUri(id, apiKey), request, Lrg.class);
  }

  public RoofIntersections computeIntersections(String id, CreateRoofIntersections request) {
    return put(buildIntersectionsUri(id), request, RoofIntersections.class);
  }

  private <T> T put(URI uri, Object request, Class<T> responseType) {
    var headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    headers.setAccept(List.of(MediaType.APPLICATION_JSON));

    var entity = new HttpEntity<>(request, headers);

    try {
      var response = restTemplate.exchange(uri, HttpMethod.PUT, entity, responseType);
      return response.getBody();
    } catch (HttpStatusCodeException e) {
      throw mapHttpError(e);
    } catch (RestClientException e) {
      throw new ThreedApiException("Unable to call Threed API : " + e.getMessage(), e);
    }
  }

  @SneakyThrows
  private URI buildGenerateUri(String id, String apiKey) {
    var builder =
        fromUri(new URI(properties.getBaseUrl())).pathSegment(PREFIX_PATH, "{id}", SUFFIX_PATH);
    var variables = new HashMap<String, Object>();
    variables.put("id", id);

    if (apiKey != null) {
      builder = builder.queryParam(API_KEY_QUERY_PARAM, "{geojobsApiKey}");
      variables.put("geojobsApiKey", apiKey);
    }

    return withThreedApiKey(builder, variables);
  }

  /** The id is the client's own and ends up in threed's path: it can not be a path itself. */
  @SneakyThrows
  private URI buildIntersectionsUri(String id) {
    if (id == null || !SAFE_ID.matcher(id).matches()) {
      throw new BadRequestException(
          "Id must be 1 to 128 letters, digits, '-' or '_' but was: " + id);
    }
    return withThreedApiKey(
        fromUri(new URI(properties.getBaseUrl())).path(INTERSECTIONS_PATH + id), Map.of());
  }

  /**
   * threed refuses any computing call without its api key. Every value goes through a template
   * variable so it is encoded, and only the path is logged so that no key ends up in the logs.
   */
  private URI withThreedApiKey(UriComponentsBuilder builder, Map<String, Object> variables) {
    log.info("Uri={}", builder.buildAndExpand(variables).getPath());
    var withApiKey = new HashMap<>(variables);
    withApiKey.put("threedApiKey", properties.getApiKey());
    return builder
        .queryParam(THREED_API_KEY_QUERY_PARAM, "{threedApiKey}")
        .encode()
        .buildAndExpand(withApiKey)
        .toUri();
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
