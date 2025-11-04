package app.bpartners.geojobs.service;

import static java.time.Instant.now;
import static java.util.UUID.randomUUID;

import app.bpartners.geojobs.endpoint.rest.model.RevokeApiKeyResponse;
import app.bpartners.geojobs.model.exception.BadRequestException;
import app.bpartners.geojobs.repository.CommunityAuthorizationRepository;
import app.bpartners.geojobs.repository.RevokedApiKeyRepository;
import app.bpartners.geojobs.repository.model.community.CommunityAuthorization;
import app.bpartners.geojobs.repository.model.community.RevokedApiKey;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RevokedApiKeyService {
  private final RevokedApiKeyRepository repository;
  private final CommunityAuthorizationRepository communityAuthRepository;

  @Transactional
  public RevokeApiKeyResponse revokeCommunityApiKey(CommunityAuthorization communityAuthorization) {
    if (communityAuthorization.isApiKeyRevoked()) {
      throw new BadRequestException("Cannot revoke apikey as it is already revoked");
    }

    var revokedApiKey =
        RevokedApiKey.builder()
            .id(randomUUID().toString())
            .revokedAt(now())
            .revokedApiKeyValue(communityAuthorization.getApiKey())
            .communityOwnerId(communityAuthorization.getId())
            .build();

    communityAuthorization.setApiKeyRevoked(true);
    repository.save(revokedApiKey);
    communityAuthRepository.save(communityAuthorization);

    return new RevokeApiKeyResponse().message("Your API key has been successfully revoked");
  }
}
