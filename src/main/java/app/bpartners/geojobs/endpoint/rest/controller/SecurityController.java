package app.bpartners.geojobs.endpoint.rest.controller;

import app.bpartners.geojobs.endpoint.rest.controller.mapper.ApiKeyMapper;
import app.bpartners.geojobs.endpoint.rest.controller.mapper.RevokedApiKeyMapper;
import app.bpartners.geojobs.endpoint.rest.model.*;
import app.bpartners.geojobs.endpoint.rest.security.AuthProvider;
import app.bpartners.geojobs.endpoint.rest.validator.RevokeApiKeyValidator;
import app.bpartners.geojobs.model.exception.ForbiddenException;
import app.bpartners.geojobs.repository.CommunityAuthorizationRepository;
import app.bpartners.geojobs.repository.model.community.CommunityAuthorization;
import app.bpartners.geojobs.service.ApiKeyService;
import app.bpartners.geojobs.service.RevokedApiKeyService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
public class SecurityController {
  private final RevokedApiKeyService service;
  private final AuthProvider authProvider;
  private final CommunityAuthorizationRepository communityAuthRepository;
  private final ApiKeyMapper apiKeyMapper;
  private final ApiKeyService apiKeyService;
  private final RevokedApiKeyMapper revokedApiKeyMapper;
  private final RevokeApiKeyValidator revokeApiKeyValidator;

  @PostMapping("/api/keys")
  public List<ApiKey> generateApiKeys(@RequestBody List<CreateApiKey> createApiKeys) {
    var communityAuthorizationList = apiKeyMapper.toCommunityAuthorization(createApiKeys);
    var savedAuthorizations = apiKeyService.generateApiKeys(communityAuthorizationList);

    return savedAuthorizations.stream()
        .map(
            authorization ->
                new ApiKey()
                    .key(authorization.apiKey())
                    .creationDatetime(authorization.creationDatetime()))
        .toList();
  }

  @DeleteMapping("/api/keys")
  public RevokedApiKey revokeSpecificApiKey(@RequestBody RevokeApiKey revokeApiKey) {
    revokeApiKeyValidator.accept(revokeApiKey);
    CommunityAuthorization communityAuthorization =
        communityAuthRepository
            .findByApiKey(authProvider.getPrincipal().getPassword())
            .orElseThrow(ForbiddenException::new);
    String keyValue = revokeApiKey.getKeyValue().toString();

    return revokedApiKeyMapper.toRest(
        service.revokeCommunityApiKey(communityAuthorization, keyValue));
  }

  /**
   * @deprecated Uses the API key stored in the community authorization table, which is part of a
   *     deprecated API key system and should no longer be used.
   */
  @Deprecated
  @DeleteMapping("/keys")
  public RevokeApiKeyResponse revokeApikey() {
    var communityAuthorization =
        communityAuthRepository
            .findByApiKey(authProvider.getPrincipal().getPassword())
            .orElseThrow(ForbiddenException::new);
    service.revokeCommunityLatestApiKey(communityAuthorization);
    log.warn(
        "This endpoint is deprecated and should no longer be used, and will be removed in a future"
            + " version.");
    return new RevokeApiKeyResponse().message("Your API key has been successfully revoked");
  }
}
