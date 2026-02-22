package app.bpartners.geojobs.service.dashboard;

import static app.bpartners.geojobs.model.exception.ApiException.ExceptionType.SERVER_EXCEPTION;
import static app.bpartners.geojobs.service.dashboard.ApiConfiguration.API_KEY_HEADER;
import static app.bpartners.geojobs.service.dashboard.component.UserApiKeyType.DASHBOARD;
import static org.springframework.http.HttpMethod.*;

import app.bpartners.geojobs.model.exception.ApiException;
import app.bpartners.geojobs.model.exception.NotFoundException;
import app.bpartners.geojobs.service.dashboard.component.Account;
import app.bpartners.geojobs.service.dashboard.component.User;
import app.bpartners.geojobs.service.dashboard.component.UserApiKey;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Component
@RequiredArgsConstructor
public class UserAccountsApi {
  private final RestTemplate restTemplate;
  private final ApiConfiguration apiConfiguration;
  private final SecurityApi securityApi;

  public List<Account> getAccountsByUserId(String userId, String apiKey) {
    String endpoint =
        String.format("%s/users/%s/accounts", apiConfiguration.getDashboardApiUrl(), userId);
    var headers = new HttpHeaders();
    headers.add(API_KEY_HEADER, apiKey);
    var requestEntity = new HttpEntity<>(headers);

    return restTemplate
        .exchange(endpoint, GET, requestEntity, new ParameterizedTypeReference<List<Account>>() {})
        .getBody();
  }

  public Account getActiveByUserId(String apiKey) {
    var accounts = getAccountsByUserId(securityApi.retrieveUserId(apiKey), apiKey);
    return accounts.stream()
        .filter(Account::active)
        .findFirst()
        .orElseGet(() -> accounts.size() > 1 ? accounts.get(1) : null);
  }

  public List<User> getUsersByCriteria(String email, Integer page, Integer size, String apiKey) {
    var endpoint = String.format("%s/users", apiConfiguration.getDashboardApiUrl());

    UriComponentsBuilder builder =
        UriComponentsBuilder.fromHttpUrl(endpoint).queryParam("email", email);

    var headers = new HttpHeaders();
    headers.add(API_KEY_HEADER, apiKey);
    var requestEntity = new HttpEntity<>(headers);

    return restTemplate
        .exchange(
            builder.build().toUri(),
            GET,
            requestEntity,
            new ParameterizedTypeReference<List<User>>() {})
        .getBody();
  }

  public List<UserApiKey> getUserApiKey(String userId, String adminApiKey) {
    var endpoint = String.format("%s/users/%s/keys", apiConfiguration.getDashboardApiUrl(), userId);

    var headers = new HttpHeaders();
    headers.add(API_KEY_HEADER, adminApiKey);
    var requestEntity = new HttpEntity<>(headers);

    return restTemplate
        .exchange(
            endpoint, GET, requestEntity, new ParameterizedTypeReference<List<UserApiKey>>() {})
        .getBody();
  }

  public UserApiKey getOrGenerateApiKey(
      String userEmail, String newUserApiKey, String adminApiKey) {
    var usersByEmail = getUsersByCriteria(userEmail, 1, 500, adminApiKey);
    if (usersByEmail.isEmpty()) {
      throw new NotFoundException(
          "Any user with email like " + userEmail + " found in BirdIA dashboard");
    } else if (usersByEmail.size() > 1) {
      throw new UnsupportedOperationException(
          "Provided email address retrieved "
              + usersByEmail.size()
              + " users. "
              + "Please choose which of following users do you want to generate new api key : "
              + usersByEmail.stream().map(User::email).toList());
    }
    var dashboardUserIdentifier = usersByEmail.getFirst().id();

    var actualApiKeyList = getUserApiKey(dashboardUserIdentifier, adminApiKey);
    UserApiKey actualApiKey =
        actualApiKeyList.stream()
            .filter(apiKey -> DASHBOARD.equals(apiKey.type()))
            .findFirst()
            .orElseThrow(
                () ->
                    new ApiException(
                        SERVER_EXCEPTION,
                        "Unable to retrieve dashboard api key for email " + userEmail));
    if (actualApiKey.key() != null) {
      return actualApiKey;
    }

    return updateUserDashboardApiKey(newUserApiKey, adminApiKey, dashboardUserIdentifier);
  }

  private UserApiKey updateUserDashboardApiKey(
      String newUserApiKey, String adminApiKey, String dashboardUserIdentifier) {
    var endpoint =
        String.format(
            "%s/users/%s/keys", apiConfiguration.getDashboardApiUrl(), dashboardUserIdentifier);
    var headers = new HttpHeaders();
    headers.add(API_KEY_HEADER, adminApiKey);
    var requestEntity = new HttpEntity<>(new UserApiKey(newUserApiKey, DASHBOARD), headers);
    return Objects.requireNonNull(
        restTemplate
            .exchange(
                endpoint, POST, requestEntity, new ParameterizedTypeReference<UserApiKey>() {})
            .getBody());
  }
}
