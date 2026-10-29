package app.bpartners.geojobs.endpoint.rest.security.authenticator;

import static app.bpartners.geojobs.endpoint.rest.security.authenticator.ApiKeyAuthenticator.API_KEY_HEADER;
import static app.bpartners.geojobs.endpoint.rest.security.model.Authority.Role.ROLE_COMMUNITY;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import app.bpartners.geojobs.endpoint.rest.security.authorizer.CommunityUserSubscriptionAuthorizer;
import app.bpartners.geojobs.repository.CommunityAuthorizationRepository;
import app.bpartners.geojobs.repository.model.community.CommunityAuthorization;
import app.bpartners.geojobs.repository.model.community.RevokedApiKey;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

class ApiKeyAuthenticatorTest {
  private static final String KEY = "key";
  private static final String OTHER_KEY = "other-key";

  private final CommunityAuthorizationRepository repository =
      mock(CommunityAuthorizationRepository.class);
  private final ApiKeyAuthenticator subject =
      new ApiKeyAuthenticator(repository, mock(CommunityUserSubscriptionAuthorizer.class));

  private CommunityAuthorization community(List<RevokedApiKey> revokedApiKeys) {
    return CommunityAuthorization.builder()
        .id("community_id")
        .role(ROLE_COMMUNITY)
        .revokedApiKeys(revokedApiKeys)
        .build();
  }

  private void authenticate(String apiKey) {
    subject.retrieveUser(
        API_KEY_HEADER, new UsernamePasswordAuthenticationToken(API_KEY_HEADER, apiKey));
  }

  @Test
  void active_key_is_accepted() {
    when(repository.findByApiKey(KEY)).thenReturn(Optional.of(community(List.of())));

    assertDoesNotThrow(() -> authenticate(KEY));
  }

  @Test
  void revoked_key_is_refused() {
    var revoked = RevokedApiKey.builder().revokedApiKeyValue(KEY).build();
    when(repository.findByApiKey(KEY)).thenReturn(Optional.of(community(List.of(revoked))));

    assertThrows(BadCredentialsException.class, () -> authenticate(KEY));
  }

  @Test
  void other_key_of_the_same_community_stays_valid_when_one_is_revoked() {
    var revoked = RevokedApiKey.builder().revokedApiKeyValue(OTHER_KEY).build();
    when(repository.findByApiKey(KEY)).thenReturn(Optional.of(community(List.of(revoked))));

    assertDoesNotThrow(() -> authenticate(KEY));
  }
}
