package app.bpartners.geojobs.service.dashboard;

import static app.bpartners.geojobs.service.dashboard.ApiConfiguration.API_KEY_HEADER;
import static org.springframework.http.HttpMethod.GET;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
@RequiredArgsConstructor
public class SecurityApi {
  private final RestTemplate restTemplate;
  private final ApiConfiguration apiConfiguration;
  private final ObjectMapper objectMapper;

  public String retrieveUserId(String apiKey) {
    return retrieveDashboardUserByApiKey(apiKey).id();
  }

  @SneakyThrows
  public DashboardUser retrieveDashboardUserByApiKey(String apiKey) {
    var endpoint = String.format("%s/whoami", apiConfiguration.getDashboardApiUrl());
    var headers = new HttpHeaders();
    headers.add(API_KEY_HEADER, apiKey);
    var requestEntity = new HttpEntity<>(headers);

    var responseBody = restTemplate.exchange(endpoint, GET, requestEntity, String.class).getBody();

    var root = objectMapper.readTree(responseBody);
    var userJsonNode = root.path("user");
    var userJsonText = userJsonNode.toString();

    return objectMapper.readValue(userJsonText, DashboardUser.class);
  }
}
