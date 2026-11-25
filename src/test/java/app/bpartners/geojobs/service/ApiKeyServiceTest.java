package app.bpartners.geojobs.service;

import static java.time.Instant.parse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import app.bpartners.geojobs.model.exception.BadRequestException;
import app.bpartners.geojobs.model.security.ApiKey;
import app.bpartners.geojobs.repository.CommunityAuthorizationRepository;
import app.bpartners.geojobs.repository.model.community.CommunityAuthorization;
import app.bpartners.geojobs.repository.model.community.CommunityAuthorizationApiKey;
import app.bpartners.geojobs.repository.model.community.RevokedApiKey;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ApiKeyServiceTest {
  private static final Instant OLD = parse("2026-01-01T00:00:00Z");
  private static final Instant NEWER = parse("2026-06-01T00:00:00Z");

  @Mock CommunityAuthorizationRepository communityAuthorizationRepository;

  @InjectMocks ApiKeyService apiKeyService;

  @BeforeEach
  void setUp() {
    lenient()
        .when(communityAuthorizationRepository.save(any()))
        .thenAnswer(invocation -> invocation.getArgument(0));
  }

  @Test
  void existing_consumer_gets_back_its_most_recent_valid_key() {
    var existing = existing("a@mail.com", key("a", "old-key", OLD), key("a", "recent-key", NEWER));
    givenStored(existing);

    var actual = apiKeyService.generateApiKeys(List.of(requested("a@mail.com", "unused-key")));

    assertEquals(List.of(new ApiKey("recent-key", NEWER)), actual);
    verify(communityAuthorizationRepository, never()).save(any());
  }

  @Test
  void every_requested_consumer_gets_a_key_when_all_of_them_exist() {
    var first = existing("a@mail.com", key("a", "key-a", OLD));
    var second = existing("b@mail.com", key("b", "key-b", NEWER));
    givenStored(first);
    givenStored(second);

    var actual =
        apiKeyService.generateApiKeys(
            List.of(requested("a@mail.com", "x"), requested("b@mail.com", "y")));

    assertEquals(List.of(new ApiKey("key-a", OLD), new ApiKey("key-b", NEWER)), actual);
  }

  @Test
  void keys_are_returned_in_request_order_for_existing_and_new_consumers() {
    var existing = existing("a@mail.com", key("a", "key-a", OLD));
    givenStored(existing);
    givenNoConsumer("new@mail.com");

    var actual =
        apiKeyService.generateApiKeys(
            List.of(requested("new@mail.com", "key-new"), requested("a@mail.com", "x")));

    assertEquals(2, actual.size());
    assertEquals("key-new", actual.get(0).apiKey());
    assertEquals(new ApiKey("key-a", OLD), actual.get(1));
  }

  @Test
  void new_consumer_is_saved_with_the_requested_key() {
    givenNoConsumer("new@mail.com");
    var requested = requested("new@mail.com", "key-new");

    var actual = apiKeyService.generateApiKeys(List.of(requested));

    assertEquals("key-new", actual.get(0).apiKey());
    verify(communityAuthorizationRepository).save(requested);
  }

  @Test
  void existing_consumer_with_only_revoked_keys_gets_a_new_key() {
    var existing = existing("a@mail.com", key("a", "revoked-key", OLD));
    existing.setRevokedApiKeys(List.of(revoked("revoked-key")));
    givenStored(existing);

    var actual = apiKeyService.generateApiKeys(List.of(requested("a@mail.com", "fresh-key")));

    assertEquals("fresh-key", actual.get(0).apiKey());
    var saved = ArgumentCaptor.forClass(CommunityAuthorization.class);
    verify(communityAuthorizationRepository).save(saved.capture());
    assertEquals("a", saved.getValue().getId());
    var fresh =
        saved.getValue().getApiKeys().stream()
            .filter(key -> key.getKeyValue().equals("fresh-key"))
            .findFirst()
            .orElseThrow();
    assertEquals("a", fresh.getCommunityOwnerId());
    assertEquals(2, saved.getValue().getApiKeys().size(), "the revoked key stays recorded");
  }

  @Test
  void dashboard_key_of_an_existing_consumer_is_updated_when_the_request_carries_another_one() {
    var existing = existing("a@mail.com", key("a", "key-a", OLD));
    existing.setDashboardApiKey("old-dashboard-key");
    givenStored(existing);
    var requested = requested("a@mail.com", "x");
    requested.setDashboardApiKey("new-dashboard-key");

    var actual = apiKeyService.generateApiKeys(List.of(requested));

    assertEquals(new ApiKey("key-a", OLD), actual.get(0));
    verify(communityAuthorizationRepository).save(existing);
    assertEquals("new-dashboard-key", existing.getDashboardApiKey());
  }

  @Test
  void request_without_any_api_key_is_rejected() {
    var requested = requested("a@mail.com", "x");
    requested.setApiKeys(List.of());

    var requests = List.of(requested);

    assertThrows(BadRequestException.class, () -> apiKeyService.generateApiKeys(requests));
  }

  private void givenStored(CommunityAuthorization stored) {
    when(communityAuthorizationRepository.findByEmail(stored.getEmail()))
        .thenReturn(Optional.of(stored));
  }

  private void givenNoConsumer(String email) {
    when(communityAuthorizationRepository.findByEmail(email)).thenReturn(Optional.empty());
  }

  private static CommunityAuthorization existing(
      String email, CommunityAuthorizationApiKey... keys) {
    return CommunityAuthorization.builder()
        .id(keys[0].getCommunityOwnerId())
        .email(email)
        .dashboardApiKey("dashboard-key")
        .apiKeys(new ArrayList<>(List.of(keys)))
        .revokedApiKeys(List.of())
        .build();
  }

  private static CommunityAuthorization requested(String email, String keyValue) {
    return CommunityAuthorization.builder()
        .id("requested-" + keyValue)
        .email(email)
        .dashboardApiKey("dashboard-key")
        .apiKeys(List.of(key("requested-" + keyValue, keyValue, Instant.now())))
        .build();
  }

  private static CommunityAuthorizationApiKey key(
      String ownerId, String keyValue, Instant createdAt) {
    return CommunityAuthorizationApiKey.builder()
        .id("id-" + keyValue)
        .communityOwnerId(ownerId)
        .keyValue(keyValue)
        .creationDatetime(createdAt)
        .build();
  }

  private static RevokedApiKey revoked(String keyValue) {
    return RevokedApiKey.builder().revokedApiKeyValue(keyValue).build();
  }
}
