package app.bpartners.geojobs.service.detection.inference;

import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.MediaType.APPLICATION_JSON;

import app.bpartners.geojobs.service.detection.DetectionResponseV2;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Component
@Slf4j
public class InferenceApiClient {
  static final String API_KEY_HEADER = "x-api-key";
  private static final String MODEL_CARDS_PATH = "/modelcards";
  private static final String INFERENCE_PATH = "/inference";

  private final RestTemplate restTemplate;
  private final String inferenceApiUrl;
  private final String inferenceApiKey;

  public InferenceApiClient(
      RestTemplate restTemplate,
      @Value("${inference.api.url}") String inferenceApiUrl,
      @Value("${inference.api.key}") String inferenceApiKey) {
    this.restTemplate = restTemplate;
    this.inferenceApiUrl = inferenceApiUrl;
    this.inferenceApiKey = inferenceApiKey;
  }

  public List<DeployedModelCard> getModelCards() {
    try {
      var response =
          restTemplate.exchange(
              inferenceApiUrl + MODEL_CARDS_PATH,
              GET,
              new HttpEntity<>(headers()),
              new ParameterizedTypeReference<List<DeployedModelCard>>() {});
      if (response.getStatusCode().value() == 200 && response.getBody() != null) {
        return response.getBody();
      }
      throw new IllegalStateException(
          "Error while listing model cards at "
              + inferenceApiUrl
              + " with HTTP code "
              + response.getStatusCode());
    } catch (RestClientException e) {
      throw new IllegalStateException(
          "Error while listing model cards at " + inferenceApiUrl + ": " + e.getMessage(), e);
    }
  }

  public DetectionResponseV2 infer(InferenceRequest request) {
    try {
      var response =
          restTemplate.postForEntity(
              inferenceApiUrl + INFERENCE_PATH,
              new HttpEntity<>(request, headers()),
              DetectionResponseV2.class);
      if (response.getStatusCode().value() == 200) {
        return response.getBody();
      }
      throw new IllegalStateException(
          "Error while running inference with model card "
              + request.modelCardId()
              + " with HTTP code "
              + response.getStatusCode());
    } catch (RestClientException e) {
      throw new IllegalStateException(
          "Error while running inference with model card "
              + request.modelCardId()
              + ": "
              + e.getMessage(),
          e);
    }
  }

  private HttpHeaders headers() {
    var headers = new HttpHeaders();
    headers.setContentType(APPLICATION_JSON);
    headers.setAccept(List.of(APPLICATION_JSON));
    headers.add(API_KEY_HEADER, inferenceApiKey);
    return headers;
  }
}
