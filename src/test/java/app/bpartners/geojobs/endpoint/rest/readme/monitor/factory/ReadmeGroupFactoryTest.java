package app.bpartners.geojobs.endpoint.rest.readme.monitor.factory;

import static app.bpartners.geojobs.conf.EnvConf.ADMIN_EMAIL;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import app.bpartners.geojobs.endpoint.rest.readme.monitor.model.ReadmeGroup;
import app.bpartners.geojobs.endpoint.rest.security.model.Principal;
import app.bpartners.geojobs.repository.CommunityAuthorizationRepository;
import app.bpartners.geojobs.repository.model.community.CommunityAuthorization;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class ReadmeGroupFactoryTest {
  private static final String ADMIN_API_KEY = "the-admin-api-key";
  private static final String ADMIN_LABEL_NAME = "admin";
  CommunityAuthorizationRepository communityAuthRepositoryMock = mock();
  Principal principalMock = mock();
  ReadmeGroupFactory subject = new ReadmeGroupFactory(ADMIN_EMAIL, communityAuthRepositoryMock);

  @Test
  void create_readme_group_from_community_principal_ok() {
    var communityAuthorization = communityAuthorization();
    when(communityAuthRepositoryMock.findByApiKey(any()))
        .thenReturn(Optional.of(communityAuthorization));
    when(principalMock.isAdmin()).thenReturn(false);
    var expected =
        ReadmeGroup.builder()
            .email(communityAuthorization.getEmail())
            .label(communityAuthorization.getName())
            .apiKey(communityAuthorization.getApiKey())
            .build();

    var actual = subject.createReadmeGroup(principalMock);

    assertEquals(expected, actual);
  }

  @Test
  void create_readme_group_from_admin_principal_ok() {
    when(principalMock.isAdmin()).thenReturn(true);
    when(principalMock.getPassword()).thenReturn(ADMIN_API_KEY);
    var expected =
        ReadmeGroup.builder()
            .email(ADMIN_EMAIL)
            .label(ADMIN_LABEL_NAME)
            .apiKey(ADMIN_API_KEY)
            .build();

    var actual = subject.createReadmeGroup(principalMock);

    assertEquals(expected, actual);
  }

  private CommunityAuthorization communityAuthorization() {
    return CommunityAuthorization.builder()
        .email("dummy@gmail.com")
        .name("dummyName")
        .apiKey("dummyApiKey")
        .build();
  }
}
