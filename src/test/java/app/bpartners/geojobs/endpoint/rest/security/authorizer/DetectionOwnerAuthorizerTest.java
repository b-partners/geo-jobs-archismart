package app.bpartners.geojobs.endpoint.rest.security.authorizer;

import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import app.bpartners.geojobs.model.exception.ForbiddenException;
import app.bpartners.geojobs.repository.model.community.CommunityAuthorization;
import app.bpartners.geojobs.repository.model.detection.Detection;
import org.junit.jupiter.api.Test;

class DetectionOwnerAuthorizerTest {
  DetectionOwnerAuthorizer subject = new DetectionOwnerAuthorizer();

  @Test
  void should_accept_if_the_given_community_is_the_owner() {
    var communityId = randomUUID().toString();
    assertDoesNotThrow(
        () ->
            subject.accept(
                CommunityAuthorization.builder().id(communityId).build(),
                Detection.builder().communityOwnerId(communityId).build()));
  }

  @Test
  void should_not_accept_if_the_given_community_is_the_owner() {
    var communityAuthorization =
        CommunityAuthorization.builder().id(randomUUID().toString()).build();
    var detection = Detection.builder().communityOwnerId(randomUUID().toString()).build();

    assertThrows(ForbiddenException.class, () -> subject.accept(communityAuthorization, detection));
  }
}
