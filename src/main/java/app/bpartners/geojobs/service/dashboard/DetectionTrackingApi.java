package app.bpartners.geojobs.service.dashboard;

import static app.bpartners.geojobs.service.dashboard.ApiConfiguration.API_KEY_HEADER;
import static org.springframework.http.HttpMethod.POST;

import app.bpartners.geojobs.service.dashboard.component.CreateDetectionTracking;
import app.bpartners.geojobs.service.dashboard.component.DetectionTracking;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
@RequiredArgsConstructor
public class DetectionTrackingApi {
  private final RestTemplate restTemplate = new RestTemplate();
  private final ApiConfiguration apiConfiguration;
  private final SecurityApi securityApi;

  public List<DetectionTracking> registerDetection(
      String apiKey, List<CreateDetectionTracking> createDetectionTracking) {
    var dashboardUserId = securityApi.retrieveUserId(apiKey);
    var endpoint =
        String.format(
            "%s/users/%s/detectionTracking",
            apiConfiguration.getDashboardApiUrl(), dashboardUserId);
    var headers = new HttpHeaders();
    headers.add(API_KEY_HEADER, apiKey);
    var requestEntity = new HttpEntity<>(createDetectionTracking, headers);

    return restTemplate
        .exchange(
            endpoint,
            POST,
            requestEntity,
            new ParameterizedTypeReference<List<DetectionTracking>>() {})
        .getBody();
  }
}
