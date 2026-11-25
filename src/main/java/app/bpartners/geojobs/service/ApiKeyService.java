package app.bpartners.geojobs.service;

import static java.time.Instant.now;
import static java.util.UUID.randomUUID;

import app.bpartners.geojobs.model.exception.BadRequestException;
import app.bpartners.geojobs.model.security.ApiKey;
import app.bpartners.geojobs.repository.CommunityAuthorizationRepository;
import app.bpartners.geojobs.repository.model.community.CommunityAuthorization;
import app.bpartners.geojobs.repository.model.community.CommunityAuthorizationApiKey;
import app.bpartners.geojobs.service.dashboard.UserAccountsApi;
import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class ApiKeyService {
  private final CommunityAuthorizationRepository communityAuthorizationRepository;
  private final UserAccountsApi userAccountsApi;
  private final String adminApiKey;

  public ApiKeyService(
      CommunityAuthorizationRepository communityAuthorizationRepository,
      UserAccountsApi userAccountsApi,
      @Value("${admin.api.key}") String adminApiKey) {
    this.communityAuthorizationRepository = communityAuthorizationRepository;
    this.userAccountsApi = userAccountsApi;
    this.adminApiKey = adminApiKey;
  }

  public List<ApiKey> generateApiKeys(List<CommunityAuthorization> authorizations) {
    return authorizations.stream().map(this::generateApiKey).toList();
  }

  private ApiKey generateApiKey(CommunityAuthorization requested) {
    var requestedApiKey = requested.getMostRecentApiKey();
    if (requestedApiKey == null) {
      throw new BadRequestException("No API key to generate for " + requested.getEmail());
    }
    var stored =
        communityAuthorizationRepository
            .findByEmail(requested.getEmail())
            .map(existing -> updateExisting(existing, requested, requestedApiKey))
            .orElseGet(() -> createNew(requested));
    var mostRecentApiKey = stored.getMostRecentApiKey();
    if (mostRecentApiKey == null) {
      throw new BadRequestException("The requested API key is revoked for " + requested.getEmail());
    }
    return new ApiKey(mostRecentApiKey.getKeyValue(), mostRecentApiKey.getCreationDatetime());
  }

  private CommunityAuthorization updateExisting(
      CommunityAuthorization existing,
      CommunityAuthorization requested,
      CommunityAuthorizationApiKey requestedApiKey) {
    var changed = false;
    if (requested.getDashboardApiKey() != null
        && !requested.getDashboardApiKey().equals(existing.getDashboardApiKey())) {
      existing.setDashboardApiKey(requested.getDashboardApiKey());
      changed = true;
    }
    if (existing.getMostRecentApiKey() == null) {
      addRequestedApiKey(existing, requestedApiKey);
      changed = true;
    }
    return changed ? communityAuthorizationRepository.save(existing) : existing;
  }

  private void addRequestedApiKey(
      CommunityAuthorization existing, CommunityAuthorizationApiKey requestedApiKey) {
    if (existing.getApiKeys() == null) {
      existing.setApiKeys(new ArrayList<>());
    }
    existing
        .getApiKeys()
        .add(
            CommunityAuthorizationApiKey.builder()
                .id(randomUUID().toString())
                .keyValue(requestedApiKey.getKeyValue())
                .communityOwnerId(existing.getId())
                .creationDatetime(now())
                .build());
  }

  private CommunityAuthorization createNew(CommunityAuthorization requested) {
    if (requested.getDashboardApiKey() == null) {
      var dashboardUserApiKey =
          userAccountsApi.getOrGenerateApiKey(
              requested.getEmail(), requested.getApiKey(), adminApiKey);
      requested.setDashboardApiKey(dashboardUserApiKey.key());
    }
    return communityAuthorizationRepository.save(requested);
  }
}
