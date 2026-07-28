package app.bpartners.geojobs.service;

import static app.bpartners.geojobs.endpoint.rest.model.CreateApiKey.ConsumerTypeEnum.INSURANCE;
import static app.bpartners.geojobs.endpoint.rest.model.ModelName.TOITURE;
import static app.bpartners.geojobs.endpoint.rest.security.model.Authority.Role.ROLE_INSURANCE;
import static app.bpartners.geojobs.repository.model.SurfaceUnit.SQUARE_METER;
import static app.bpartners.geojobs.service.dashboard.component.UserApiKeyType.DASHBOARD;
import static java.time.Instant.now;
import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import app.bpartners.geojobs.conf.FacadeIT;
import app.bpartners.geojobs.endpoint.rest.controller.mapper.RevokedApiKeyMapper;
import app.bpartners.geojobs.endpoint.rest.controller.v1.SecurityController;
import app.bpartners.geojobs.endpoint.rest.model.CreateApiKey;
import app.bpartners.geojobs.endpoint.rest.model.DetectableObjectModel;
import app.bpartners.geojobs.endpoint.rest.model.RevokeApiKey;
import app.bpartners.geojobs.endpoint.rest.security.AuthProvider;
import app.bpartners.geojobs.endpoint.rest.security.model.Authority;
import app.bpartners.geojobs.endpoint.rest.security.model.Principal;
import app.bpartners.geojobs.repository.CommunityAuthorizationApiKeyRepository;
import app.bpartners.geojobs.repository.CommunityAuthorizationRepository;
import app.bpartners.geojobs.repository.RevokedApiKeyRepository;
import app.bpartners.geojobs.repository.model.community.CommunityAuthorization;
import app.bpartners.geojobs.repository.model.community.CommunityAuthorizationApiKey;
import app.bpartners.geojobs.service.dashboard.UserAccountsApi;
import app.bpartners.geojobs.service.dashboard.component.UserApiKey;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.transaction.annotation.Transactional;

class SecurityControllerIT extends FacadeIT {
  @Autowired SecurityController subject;
  @Autowired CommunityAuthorizationRepository authorizationRepository;
  @Autowired CommunityAuthorizationApiKeyRepository apiKeyRepository;
  @Autowired RevokedApiKeyRepository revokedApiKeyRepository;
  @Autowired RevokedApiKeyMapper revokedApiKeyMapper;
  @MockBean UserAccountsApi userAccountsApiMock;
  @MockBean AuthProvider authProviderMock;

  @Transactional
  @Test
  void generate_and_read_api_keys_for_insurance_ok() {
    when(userAccountsApiMock.getOrGenerateApiKey(any(), any(), any()))
        .thenAnswer(invocationOnMock -> new UserApiKey(invocationOnMock.getArgument(1), DASHBOARD));
    var consumerEmail = "randomEmail" + randomUUID();

    var actual = subject.generateApiKeys(List.of(someCreateApiKey(consumerEmail)));

    assertEquals(1, actual.size());
    var actualKey = actual.getFirst().getKey();
    var actualCommunity = authorizationRepository.findByDashboardApiKey(actualKey).orElse(null);
    assertEquals(
        CommunityAuthorization.builder()
            .id(actualCommunity.getId())
            .apiKey(actualKey)
            .apiKeys(actualCommunity.getApiKeys())
            .creationDatetime(actualCommunity.getCreationDatetime())
            .name("dummyConsumerName")
            .email(consumerEmail)
            .detectableModels(List.of(TOITURE))
            .role(ROLE_INSURANCE)
            .maxSurfaceUnit(SQUARE_METER)
            .authorizedZones(List.of())
            .dashboardApiKey(actualKey)
            .build(),
        actualCommunity);
    assertTrue(actualCommunity.getAuthorizedZones().isEmpty());
  }

  @Test
  void used_email_return_existing_keys() {
    when(userAccountsApiMock.getOrGenerateApiKey(any(), any(), any()))
        .thenAnswer(invocationOnMock -> new UserApiKey(invocationOnMock.getArgument(1), DASHBOARD));
    var consumerEmail = "randomEmail" + randomUUID();
    assertDoesNotThrow(() -> subject.generateApiKeys(List.of(someCreateApiKey(consumerEmail))));

    var actual = subject.generateApiKeys(List.of(someCreateApiKey(consumerEmail)));

    System.out.println(actual);
  }

  @Transactional
  @Test
  void revoke_api_keys_ok() {
    var authentificationApiKey = randomUUID().toString();
    var community = authorizationRepository.save(communityAuthorization(authentificationApiKey));
    var apiKeyToRevoke = randomUUID().toString();
    var apiKeyToRevokeEntity = apiKeyRepository.save(apiKeyToRevoke(apiKeyToRevoke, community));
    community.setApiKeys(new ArrayList<>(List.of(apiKeyToRevokeEntity)));
    authorizationRepository.save(community);
    when(authProviderMock.getPrincipal())
        .thenReturn(new Principal(authentificationApiKey, Set.of(new Authority(ROLE_INSURANCE))));

    var actual =
        subject.revokeSpecificApiKey(new RevokeApiKey().keyValue(UUID.fromString(apiKeyToRevoke)));

    var revokedApiKey =
        revokedApiKeyRepository.findByRevokedApiKeyValue(apiKeyToRevoke).orElseThrow();
    assertEquals(revokedApiKeyMapper.toRest(revokedApiKey), actual);
    assertEquals(community.getId(), revokedApiKey.getCommunityOwnerId());
    assertEquals(apiKeyToRevoke, revokedApiKey.getRevokedApiKeyValue());
  }

  private static CommunityAuthorizationApiKey apiKeyToRevoke(
      String apiKeyToRevoke, CommunityAuthorization community) {
    return CommunityAuthorizationApiKey.builder()
        .id("secondary-id")
        .keyValue(apiKeyToRevoke)
        .communityOwnerId(community.getId())
        .build();
  }

  private static CommunityAuthorization communityAuthorization(String apiKey) {
    return CommunityAuthorization.builder()
        .id("community-id")
        .name("community-name")
        .email("email@gmail.com")
        .dashboardApiKey("dashboard-" + apiKey)
        .apiKeys(
            List.of(new CommunityAuthorizationApiKey("api-key-id", "community-id", apiKey, now())))
        .role(ROLE_INSURANCE)
        .isApiKeyRevoked(false)
        .maxSurfaceUnit(SQUARE_METER)
        .build();
  }

  private static CreateApiKey someCreateApiKey(String consumerEmail) {
    return new CreateApiKey()
        .consumerName("dummyConsumerName")
        .consumerEmail(consumerEmail)
        .consumerType(INSURANCE)
        .detectableObjectModel(new DetectableObjectModel().modelName(TOITURE))
        .maxSurface(null)
        .authorizedZones(List.of());
  }
}
