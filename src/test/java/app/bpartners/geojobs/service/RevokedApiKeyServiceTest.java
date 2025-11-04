package app.bpartners.geojobs.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import app.bpartners.geojobs.endpoint.rest.model.RevokeApiKeyResponse;
import app.bpartners.geojobs.model.exception.BadRequestException;
import app.bpartners.geojobs.repository.CommunityAuthorizationRepository;
import app.bpartners.geojobs.repository.RevokedApiKeyRepository;
import app.bpartners.geojobs.repository.model.community.CommunityAuthorization;
import app.bpartners.geojobs.repository.model.community.RevokedApiKey;
import org.junit.jupiter.api.Test;

class RevokedApiKeyServiceTest {
  RevokedApiKeyRepository revokedApiKeyRepositoryMock = mock();
  CommunityAuthorizationRepository communityAuthRepositoryMock = mock();
  RevokedApiKeyService subject =
      new RevokedApiKeyService(revokedApiKeyRepositoryMock, communityAuthRepositoryMock);

  @Test
  void cannot_revoked_api_key_if_already_revoked() {
    var communityAuthorization = communityAuthorization(true);
    var error =
        assertThrows(
            BadRequestException.class, () -> subject.revokeCommunityApiKey(communityAuthorization));
    assertEquals("Cannot revoke apikey as it is already revoked", error.getMessage());
  }

  @Test
  void can_revoke_api_key_ok() {
    var communityAuthorization = communityAuthorization(false);
    var expected = new RevokeApiKeyResponse().message("Your API key has been successfully revoked");
    when(communityAuthRepositoryMock.save(any(CommunityAuthorization.class))).thenReturn(mock());
    when(revokedApiKeyRepositoryMock.save(any(RevokedApiKey.class))).thenReturn(mock());

    var actual = subject.revokeCommunityApiKey(communityAuthorization);

    assertEquals(expected, actual);
    verify(revokedApiKeyRepositoryMock, times(1)).save(any(RevokedApiKey.class));
    verify(communityAuthRepositoryMock, times(1)).save(communityAuthorization(true));
  }

  CommunityAuthorization communityAuthorization(boolean isApiKeyRevoked) {
    return CommunityAuthorization.builder()
        .id("communityId")
        .name("communityName")
        .apiKey("communityApiKey")
        .email("myemail@gmail.com")
        .maxSurface(1_000)
        .isApiKeyRevoked(isApiKeyRevoked)
        .build();
  }
}
