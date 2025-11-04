package app.bpartners.geojobs.endpoint.rest.readme.webhook;

import static app.bpartners.geojobs.endpoint.rest.readme.monitor.factory.ReadmeGroupFactory.ADMIN_LABEL_NAME;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import app.bpartners.geojobs.endpoint.rest.readme.webhook.model.SingleUserInfo;
import app.bpartners.geojobs.repository.CommunityAuthorizationRepository;
import app.bpartners.geojobs.repository.model.community.CommunityAuthorization;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class ReadmeWebhookServiceTest {
  private static final String ADMIN_EMAIL = "admin@gmail.com";
  private static final String ADMIN_API_KEY = "adminApiKey";
  CommunityAuthorizationRepository communityAuthRepositoryMock = mock();
  ReadmeWebhookService subject =
      new ReadmeWebhookService(ADMIN_EMAIL, ADMIN_API_KEY, communityAuthRepositoryMock);

  @Test
  void retrieve_community_info() {
    var communityAuthorization = communityAuthorization();
    var expected =
        SingleUserInfo.builder()
            .isAdmin(false)
            .apiKey(communityAuthorization.getApiKey())
            .email(communityAuthorization.getEmail())
            .name(communityAuthorization.getName())
            .build();
    when(communityAuthRepositoryMock.findByEmail(any()))
        .thenReturn(Optional.of(communityAuthorization));

    var actual = subject.retrieveUserInfoByEmail(communityAuthorization.getEmail());

    assertEquals(expected, actual);
  }

  @Test
  void retrieve_admin_info() {
    var expected =
        SingleUserInfo.builder()
            .isAdmin(true)
            .apiKey(ADMIN_API_KEY)
            .email(ADMIN_EMAIL)
            .name(ADMIN_LABEL_NAME)
            .build();

    var actual = subject.retrieveUserInfoByEmail(ADMIN_EMAIL);

    assertEquals(expected, actual);
  }

  private CommunityAuthorization communityAuthorization() {
    return CommunityAuthorization.builder()
        .id("communityId")
        .email("community@gmail.com")
        .name("communityName")
        .apiKey("communityApiKey")
        .build();
  }
}
