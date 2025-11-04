package app.bpartners.geojobs.endpoint.rest.readme.webhook;

import static app.bpartners.geojobs.endpoint.rest.readme.monitor.factory.ReadmeGroupFactory.ADMIN_LABEL_NAME;

import app.bpartners.geojobs.endpoint.rest.readme.webhook.model.SingleUserInfo;
import app.bpartners.geojobs.model.exception.ForbiddenException;
import app.bpartners.geojobs.repository.CommunityAuthorizationRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class ReadmeWebhookService {
  private final CommunityAuthorizationRepository communityAuthorizationRepository;
  private final String adminEmail;
  private final String adminApiKey;

  public ReadmeWebhookService(
      @Value("${admin.email}") String adminEmail,
      @Value("${admin.api.key}") String adminApiKey,
      CommunityAuthorizationRepository communityAuthorizationRepository) {
    this.adminEmail = adminEmail;
    this.adminApiKey = adminApiKey;
    this.communityAuthorizationRepository = communityAuthorizationRepository;
  }

  public SingleUserInfo retrieveUserInfoByEmail(String email) {
    if (adminEmail.equals(email)) {
      return SingleUserInfo.builder()
          .isAdmin(true)
          .name(ADMIN_LABEL_NAME)
          .email(adminEmail)
          .apiKey(adminApiKey)
          .build();
    }

    var user =
        communityAuthorizationRepository.findByEmail(email).orElseThrow(ForbiddenException::new);
    return SingleUserInfo.builder()
        .isAdmin(false)
        .name(user.getName())
        .email(user.getEmail())
        .apiKey(user.getApiKey())
        .build();
  }
}
