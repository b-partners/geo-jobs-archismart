package app.bpartners.geojobs.service.roofer3dbag;

import app.bpartners.geojobs.service.roofer3dbag.conf.RooferApiProperties;
import app.bpartners.geojobs.service.roofer3dbag.exception.RooferApiException;
import app.bpartners.geojobs.service.roofer3dbag.model.CityJsonGenerationRequest;
import app.bpartners.geojobs.service.roofer3dbag.model.CityJsonGenerationResponse;
import app.bpartners.geojobs.service.roofer3dbag.model.ErrorResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Client pour l'API 3DBag/roofer.
 *
 * <p>Exemple :
 *
 * <pre>{@code
 * CityJsonGenerationRequest req = CityJsonGenerationRequest.builder()
 *     .geoJsonBuildingPresignedUrl("s3://bucket/emprise.geojson")
 *     .lidarPresignedUrls(List.of("s3://bucket/lidar1.copc.laz"))
 *     .build();
 *
 * CityJsonGenerationResponse res = rooferClient.generateCityJson(req, 0.66f);
 * }</pre>
 */
@Slf4j
@Component
public class Roofer3DBagApiClient {

  private static final String GENERATE_PATH = "/cityjson/generate";
  private final RestTemplate restTemplate;
  private final RooferApiProperties properties;
  private final ObjectMapper objectMapper;
  private final Float complexityFactor;

  public Roofer3DBagApiClient(
      @Qualifier("rooferRestTemplate") RestTemplate restTemplate,
      RooferApiProperties properties,
      ObjectMapper objectMapper,
      @Value("${roofer.3d.bag.complexity.factor}") Float complexityFactor) {
    this.restTemplate = restTemplate;
    this.properties = properties;
    this.objectMapper = objectMapper;
    this.complexityFactor = complexityFactor;
  }

  /**
   * Génère un CityJSON 3D à partir d'un GeoJSON d'emprise et de fichiers LiDAR.
   *
   * @param request corps de la requête (geoJsonBuildingPresignedUrl + lidarPresignedUrls
   *     obligatoires)
   * @param complexityFactor facteur de complexité optionnel (peut être null)
   * @return réponse contenant l'URL pré-signée et sa date d'expiration
   * @throws RooferApiException en cas d'erreur HTTP ou réseau
   */
  private CityJsonGenerationResponse generate(
      CityJsonGenerationRequest request, Float complexityFactor) {
    URI uri = buildGenerateUri(complexityFactor);

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    headers.setAccept(java.util.List.of(MediaType.APPLICATION_JSON));

    HttpEntity<CityJsonGenerationRequest> entity = new HttpEntity<>(request, headers);

    log.debug("POST {} (complexityFactor={})", uri, complexityFactor);

    try {
      ResponseEntity<CityJsonGenerationResponse> response =
          restTemplate.postForEntity(uri, entity, CityJsonGenerationResponse.class);
      return response.getBody();
    } catch (HttpStatusCodeException e) {
      throw mapHttpError(e);
    } catch (RestClientException e) {
      throw new RooferApiException("Unable to call 3D Bag roofer API : " + e.getMessage(), e);
    }
  }

  public CityJsonGenerationResponse generateCityJson(
      CityJsonGenerationRequest request, Float customComplexityFactor) {
    return generate(
        request, customComplexityFactor == null ? complexityFactor : customComplexityFactor);
  }

  @SneakyThrows
  private URI buildGenerateUri(Float complexityFactor) {
    UriComponentsBuilder builder =
        UriComponentsBuilder.fromUri(new URI(properties.getBaseUrl())).path(GENERATE_PATH);

    if (complexityFactor != null) {
      builder.queryParam("complexityFactor", complexityFactor);
    }
    return builder.build().toUri();
  }

  private RooferApiException mapHttpError(HttpStatusCodeException e) {
    String body = e.getResponseBodyAsString();
    String apiError = null;
    try {
      if (!body.isBlank()) {
        ErrorResponse parsed = objectMapper.readValue(body, ErrorResponse.class);
        apiError = parsed.getError();
      }
    } catch (Exception parseEx) {
      log.debug("Unable to parse response : {}", parseEx.getMessage());
    }

    String message =
        String.format(
            "API roofer error (HTTP %s) : %s",
            e.getStatusCode(), apiError != null ? apiError : body);
    return new RooferApiException(e.getStatusCode(), apiError, message);
  }
}
