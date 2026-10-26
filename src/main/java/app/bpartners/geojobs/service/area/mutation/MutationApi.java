package app.bpartners.geojobs.service.area.mutation;

import static org.apache.commons.io.FileUtils.readFileToByteArray;

import app.bpartners.geojobs.service.area.mutation.model.MutationRequest;
import app.bpartners.geojobs.service.area.mutation.model.MutationResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Base64;
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
public class MutationApi {

  private final ObjectMapper om;
  private final RestTemplate restTemplate;
  private final String mutationApiUrl;

  public MutationApi(
      ObjectMapper om,
      RestTemplate restTemplate,
      @Value("${mutation.detection.api.url}") String mutationApiUrl) {
    this.om = om;
    this.restTemplate = restTemplate;
    this.mutationApiUrl = mutationApiUrl;
  }

  @SneakyThrows
  public MutationResponse detectMutation(
      File beforeImageFile, File afterImageFile, File maskImageFile, String filename) {
    var base64Old = Base64.getEncoder().encodeToString(readFileToByteArray(beforeImageFile));
    var base64New = Base64.getEncoder().encodeToString(readFileToByteArray(afterImageFile));
    var base64Mask = Base64.getEncoder().encodeToString(readFileToByteArray(maskImageFile));

    var payload = new MutationRequest(base64Old, base64New, base64Mask, filename);
    return detectMutation(payload);
  }

  @SneakyThrows
  private MutationResponse detectMutation(MutationRequest payload) {
    log.info(
        "Calling mutation detection API: url={}, filename={}, base64OldLen={}, base64NewLen={},"
            + " base64MaskLen={}",
        mutationApiUrl,
        payload.filename(),
        payload.base64Old().length(),
        payload.base64New().length(),
        payload.base64Mask().length());

    var headers = new HttpHeaders();
    headers.add("Content-Type", "application/json");
    var requestBody = om.writeValueAsString(payload);
    var request = new HttpEntity<>(requestBody, headers);

    try {
      var uriBuilder = UriComponentsBuilder.fromUri(new URI(mutationApiUrl + "/mutation"));
      var response =
          restTemplate.postForEntity(uriBuilder.toUriString(), request, MutationResponse.class);
      if (response.getStatusCode().value() == 200) {
        var body = response.getBody();
        log.info(
            "Mutation detection API responded: filename={}, status={}, mutation={}",
            payload.filename(),
            body == null ? null : body.status(),
            body == null ? null : body.mutation());
        return body;
      }
      throw new IllegalStateException(
          "Error while calling API for mutation detection at "
              + mutationApiUrl
              + " with HTTP code "
              + response.getStatusCode());
    } catch (URISyntaxException | HttpStatusCodeException e) {
      throw new IllegalStateException(
          "Error while calling API for mutation detection at "
              + mutationApiUrl
              + " with exception "
              + e.getMessage(),
          e);
    }
  }
}
