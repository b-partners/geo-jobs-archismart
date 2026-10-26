package app.bpartners.geojobs.service.area.mutation;

import app.bpartners.geojobs.service.area.mutation.model.AreaPictureHistoryResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.URISyntaxException;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Component
@Slf4j
public class GeodataApi {

  private final ObjectMapper om;
  private final RestTemplate restTemplate;
  private final String geodataApiUrl;

  public GeodataApi(
      ObjectMapper om,
      RestTemplate restTemplate,
      @Value("${geodata.api.url}") String geodataApiUrl) {
    this.om = om;
    this.restTemplate = restTemplate;
    this.geodataApiUrl = geodataApiUrl;
  }

  /**
   * Returns the two most relevant area pictures for the given position, coming from distinct image
   * dates and ordered most recent first.
   */
  @SneakyThrows
  public AreaPictureHistoryResponse getAreaPictureHistory(double longitude, double latitude) {
    var headers = new HttpHeaders();
    headers.add("Content-Type", "application/json");
    var requestBody = om.writeValueAsString(new AreaPictureHistoryRequest(longitude, latitude));
    var request = new HttpEntity<>(requestBody, headers);

    try {
      var uriBuilder = UriComponentsBuilder.fromUri(new URI(geodataApiUrl + "/areaPictureHistory"));
      var response =
          restTemplate.postForEntity(
              uriBuilder.toUriString(), request, AreaPictureHistoryResponse.class);
      if (response.getStatusCode().value() == 200) {
        return response.getBody();
      }
      throw new IllegalStateException(
          "Error while calling API for area picture history at "
              + geodataApiUrl
              + " with HTTP code "
              + response.getStatusCode());
    } catch (URISyntaxException | HttpStatusCodeException e) {
      throw new IllegalStateException(
          "Error while calling API for area picture history at "
              + geodataApiUrl
              + " with exception "
              + e.getMessage(),
          e);
    }
  }

  private record AreaPictureHistoryRequest(double longitude, double latitude) {}
}
