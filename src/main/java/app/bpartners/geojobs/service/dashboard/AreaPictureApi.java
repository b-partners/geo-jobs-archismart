package app.bpartners.geojobs.service.dashboard;

import static app.bpartners.geojobs.service.dashboard.ApiConfiguration.API_KEY_HEADER;
import static org.springframework.http.HttpMethod.PUT;

import app.bpartners.geojobs.service.dashboard.component.AreaPictureDetails;
import app.bpartners.geojobs.service.dashboard.component.CrupdateAreaPictureDetails;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Component
@RequiredArgsConstructor
public class AreaPictureApi {
  private final RestTemplate restTemplate;
  private final ApiConfiguration apiConfiguration;
  private final UserAccountsApi userAccountsApi;

  public AreaPictureDetails crupdateAreaPictureDetails(
      String areaPictureId, CrupdateAreaPictureDetails crupdateAreaPictureDetails, String apiKey) {
    var account = userAccountsApi.getActiveByUserId(apiKey);
    var endpoint =
        String.format(
            "%s/accounts/%s/areaPictures/%s",
            apiConfiguration.getDashboardApiUrl(), account.id(), areaPictureId);
    var headers = new HttpHeaders();
    headers.add(API_KEY_HEADER, apiKey);
    var requestEntity = new HttpEntity<>(crupdateAreaPictureDetails, headers);

    AreaPictureDetails response;
    try {
      response =
          restTemplate.exchange(endpoint, PUT, requestEntity, AreaPictureDetails.class).getBody();
    } catch (HttpClientErrorException | HttpServerErrorException e) {
      log.info(
          "Error during converting address {} with payload {}",
          crupdateAreaPictureDetails.address(),
          crupdateAreaPictureDetails);
      throw e;
    }
    return response;
  }
}
