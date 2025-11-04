package app.bpartners.geojobs.endpoint.rest.readme.monitor.factory;

import app.bpartners.geojobs.endpoint.rest.readme.monitor.model.ReadmeGroup;
import app.bpartners.geojobs.endpoint.rest.security.model.Principal;
import app.bpartners.geojobs.model.exception.ForbiddenException;
import app.bpartners.geojobs.repository.CommunityAuthorizationRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class ReadmeGroupFactory {
  public static final String ADMIN_LABEL_NAME = "admin";
  private final String adminEmail;
  private final CommunityAuthorizationRepository communityAuthRepository;

  public ReadmeGroupFactory(
      @Value("${admin.email}") String adminEmail,
      CommunityAuthorizationRepository communityAuthRepository) {
    this.adminEmail = adminEmail;
    this.communityAuthRepository = communityAuthRepository;
  }

  public ReadmeGroup createReadmeGroup(Principal principal) {
    if (principal.isAdmin()) {
      return ReadmeGroup.builder()
          .label(ADMIN_LABEL_NAME)
          .apiKey(principal.getPassword())
          .email(adminEmail)
          .build();
    }
    var communityAuthorization =
        communityAuthRepository
            .findByApiKey(principal.getPassword())
            .orElseThrow(ForbiddenException::new);

    return ReadmeGroup.builder()
        .apiKey(communityAuthorization.getApiKey())
        .email(communityAuthorization.getEmail())
        .label(communityAuthorization.getName())
        .build();
  }
}
