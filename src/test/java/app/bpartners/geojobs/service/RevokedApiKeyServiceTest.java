package app.bpartners.geojobs.service;

import static app.bpartners.geojobs.service.RevokedApiKeyService.hide;
import static java.time.Instant.now;
import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import app.bpartners.geojobs.model.exception.BadRequestException;
import app.bpartners.geojobs.model.exception.NotFoundException;
import app.bpartners.geojobs.repository.CommunityAuthorizationRepository;
import app.bpartners.geojobs.repository.RevokedApiKeyRepository;
import app.bpartners.geojobs.repository.model.community.CommunityAuthorization;
import app.bpartners.geojobs.repository.model.community.CommunityAuthorizationApiKey;
import app.bpartners.geojobs.repository.model.community.RevokedApiKey;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class RevokedApiKeyServiceTest {
  RevokedApiKeyRepository revokedApiKeyRepositoryMock = mock();
  CommunityAuthorizationRepository communityAuthRepositoryMock = mock();
  CommunityAuthorizationApiKeyService communityAuthorizationApiKeyService = mock();
  RevokedApiKeyService subject =
      new RevokedApiKeyService(
          revokedApiKeyRepositoryMock,
          communityAuthRepositoryMock,
          communityAuthorizationApiKeyService);

  @Test
  void revoke_community_api_key_ok() {
    var communityAuthorization = communityAuthorizationWithApiKeys();
    var keyToRevoke = communityAuthorization.getApiKeys().getFirst();
    var revokedApiKey = revokedApiKey(keyToRevoke);
    when(communityAuthorizationApiKeyService.revokeApiKey(keyToRevoke)).thenReturn(revokedApiKey);
    when(communityAuthRepositoryMock.findByApiKey(keyToRevoke.getKeyValue()))
        .thenReturn(Optional.of(communityAuthorization));

    var actual = subject.revokeCommunityApiKey(communityAuthorization, keyToRevoke.getKeyValue());

    assertEquals(revokedApiKey, actual);
  }

  @Test
  void revoke_community_raw_api_key_ok() {
    var communityAuthorization = communityAuthorization(false);
    var keyToRevoke = communityAuthorization.getApiKey();
    when(revokedApiKeyRepositoryMock.save(any(RevokedApiKey.class))).thenReturn(mock());
    when(communityAuthRepositoryMock.save(communityAuthorization)).thenReturn(mock());
    when(communityAuthRepositoryMock.findByApiKey(keyToRevoke))
        .thenReturn(Optional.of(communityAuthorization));

    var actual = subject.revokeCommunityApiKey(communityAuthorization, keyToRevoke);

    assertEquals(keyToRevoke, actual.getRevokedApiKeyValue());
  }

  @Test
  void api_key_not_found() {
    var communityAuthorization = communityAuthorization(false);
    var keyToRevoke = communityAuthorization.getApiKey();
    when(communityAuthRepositoryMock.findByApiKey(keyToRevoke)).thenReturn(Optional.empty());

    var actual =
        assertThrows(
            NotFoundException.class,
            () -> subject.revokeCommunityApiKey(communityAuthorization, keyToRevoke));

    assertEquals("The API key " + hide(keyToRevoke) + " was not found.", actual.getMessage());
  }

  @Test
  void cannot_revoked_api_key_if_already_revoked() {
    var communityAuthorization = communityAuthorization(true);
    var error =
        assertThrows(
            BadRequestException.class,
            () -> subject.revokeCommunityLatestApiKey(communityAuthorization));
    assertEquals("Cannot revoke apikey as it is already revoked", error.getMessage());
  }

  @Test
  void can_revoke_api_key_ok() {
    var communityAuthorization = communityAuthorization(false);
    when(communityAuthRepositoryMock.save(any(CommunityAuthorization.class))).thenReturn(mock());
    when(revokedApiKeyRepositoryMock.save(any(RevokedApiKey.class))).thenReturn(mock());

    var actual = subject.revokeCommunityLatestApiKey(communityAuthorization);

    assertEquals(communityAuthorization.getApiKey(), actual.getRevokedApiKeyValue());
    verify(revokedApiKeyRepositoryMock, times(1)).save(any(RevokedApiKey.class));
    verify(communityAuthRepositoryMock, times(1)).save(communityAuthorization(true));
  }

  CommunityAuthorization communityAuthorization(boolean isApiKeyRevoked) {
    return CommunityAuthorization.builder()
        .id("communityId")
        .name("communityName")
        .apiKey("communityApiKey")
        .apiKeys(List.of())
        .email("myemail@gmail.com")
        .maxSurface(1_000)
        .isApiKeyRevoked(isApiKeyRevoked)
        .build();
  }

  static CommunityAuthorization communityAuthorizationWithApiKeys() {
    return CommunityAuthorization.builder()
        .id("communityId")
        .name("communityName")
        .apiKeys(List.of(communityAuthorizationApiKey(1), communityAuthorizationApiKey(2)))
        .email("myemail@gmail.com")
        .maxSurface(1_000)
        .isApiKeyRevoked(false)
        .build();
  }

  static CommunityAuthorizationApiKey communityAuthorizationApiKey(int number) {
    return CommunityAuthorizationApiKey.builder()
        .id("communityAuthorizationApiKeyId" + number)
        .communityOwnerId("communityId" + number)
        .keyValue("communityApiKeyValue" + number)
        .creationDatetime(now())
        .build();
  }

  static RevokedApiKey revokedApiKey(CommunityAuthorizationApiKey communityAuthorizationApiKey) {
    return RevokedApiKey.builder()
        .id(randomUUID().toString())
        .revokedApiKeyValue(communityAuthorizationApiKey.getKeyValue())
        .communityOwnerId(communityAuthorizationApiKey.getCommunityOwnerId())
        .revokedAt(now())
        .build();
  }
}
