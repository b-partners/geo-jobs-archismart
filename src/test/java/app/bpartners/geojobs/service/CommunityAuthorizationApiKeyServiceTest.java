package app.bpartners.geojobs.service;

import static java.time.Instant.now;
import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import app.bpartners.geojobs.model.exception.BadRequestException;
import app.bpartners.geojobs.repository.RevokedApiKeyRepository;
import app.bpartners.geojobs.repository.model.community.CommunityAuthorizationApiKey;
import app.bpartners.geojobs.repository.model.community.RevokedApiKey;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class CommunityAuthorizationApiKeyServiceTest {
  private static final String KEY_VALUE = randomUUID().toString();
  private final RevokedApiKeyRepository revokedApiKeyRepository = mock();

  CommunityAuthorizationApiKeyService subject =
      new CommunityAuthorizationApiKeyService(revokedApiKeyRepository);

  @Test
  void revoking_already_revoked_key_is_illegal() {
    CommunityAuthorizationApiKey apiKey = apiKey();
    when(revokedApiKeyRepository.findByRevokedApiKeyValue(apiKey.getKeyValue()))
        .thenReturn(Optional.of(revokedApiKey()));

    var actual = assertThrows(BadRequestException.class, () -> subject.revokeApiKey(apiKey));

    assertEquals("ApiKey already revoked", actual.getMessage());
  }

  @Test
  void key_revocation_integrity() {
    CommunityAuthorizationApiKey apiKey = apiKey();
    when(revokedApiKeyRepository.findByRevokedApiKeyValue(apiKey.getKeyValue()))
        .thenReturn(Optional.empty());
    when(revokedApiKeyRepository.save(any())).thenReturn(revokedApiKey());

    var actual = subject.revokeApiKey(apiKey);

    verify(revokedApiKeyRepository, times(1)).save(any());
    assertEquals(apiKey.getKeyValue(), actual.getRevokedApiKeyValue());
  }

  private static CommunityAuthorizationApiKey apiKey() {
    return CommunityAuthorizationApiKey.builder()
        .id(randomUUID().toString())
        .keyValue(KEY_VALUE)
        .build();
  }

  private static RevokedApiKey revokedApiKey() {
    return RevokedApiKey.builder().revokedApiKeyValue(KEY_VALUE).revokedAt(now()).build();
  }
}
