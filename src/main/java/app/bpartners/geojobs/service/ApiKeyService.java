package app.bpartners.geojobs.service;

import app.bpartners.geojobs.model.exception.BadRequestException;
import app.bpartners.geojobs.model.security.ApiKey;
import app.bpartners.geojobs.repository.CommunityAuthorizationRepository;
import app.bpartners.geojobs.repository.model.community.CommunityAuthorization;
import app.bpartners.geojobs.service.dashboard.UserAccountsApi;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
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
    validateAuthorizations(authorizations);

    authorizations.forEach(
        authorization -> {
          var dashboardUserApiKey =
              userAccountsApi.updateApiKey(
                  authorization.getEmail(), authorization.getApiKey(), adminApiKey);
          authorization.setApiKey(dashboardUserApiKey.key());
        });

    return communityAuthorizationRepository.saveAll(authorizations).stream()
        .map(
            communityAuthorization ->
                new ApiKey(
                    communityAuthorization.getApiKey(),
                    communityAuthorization.getCreationDatetime()))
        .collect(Collectors.toList());
  }

  private void validateAuthorizations(List<CommunityAuthorization> authorizations) {
    var exceptionMessages =
        authorizations.stream()
            .map(
                communityAuthorization -> {
                  var optionalCommunityAuthorization =
                      communityAuthorizationRepository.findByEmail(
                          communityAuthorization.getEmail());
                  if (optionalCommunityAuthorization.isPresent()) {
                    return "Email=" + communityAuthorization.getEmail() + " is already used";
                  }
                  return null;
                })
            .filter(Objects::nonNull)
            .toList();
    if (!exceptionMessages.isEmpty()) {
      throw new BadRequestException(String.join(". ", exceptionMessages));
    }
  }
}
