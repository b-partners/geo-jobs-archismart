package app.bpartners.geojobs.endpoint.rest.security.authorizer;

import static app.bpartners.geojobs.repository.model.detection.DetectableType.PASSAGE_PIETON;
import static org.junit.jupiter.api.Assertions.*;

import app.bpartners.geojobs.endpoint.rest.controller.mapper.DetectableObjectTypeMapper;
import app.bpartners.geojobs.endpoint.rest.model.DetectableObjectType;
import app.bpartners.geojobs.model.exception.ForbiddenException;
import app.bpartners.geojobs.repository.model.community.CommunityAuthorization;
import app.bpartners.geojobs.repository.model.community.CommunityDetectableObjectType;
import java.util.List;
import org.junit.jupiter.api.Test;

class CommunityDetectableObjectTypeTest {
  DetectableObjectTypeMapper detectableObjectTypeMapper = new DetectableObjectTypeMapper();
  CommunityDetectableObjectTypeAuthorizer subject =
      new CommunityDetectableObjectTypeAuthorizer(detectableObjectTypeMapper);

  @Test
  void accept_if_authorization_have_authorization_on_object_type() {
    var communityAuthorization = communityAuthorization();

    assertDoesNotThrow(
        () -> {
          subject.accept(communityAuthorization, DetectableObjectType.PASSAGE_PIETON);
        });
  }

  @Test
  void should_throws_if_object_type_is_not_authorized() {
    var communityAuthorization = communityAuthorization();

    var error =
        assertThrows(
            ForbiddenException.class,
            () -> {
              subject.accept(communityAuthorization, DetectableObjectType.PISCINE);
            });
    assertTrue(error.getMessage().contains("PISCINE"));
  }

  private CommunityAuthorization communityAuthorization() {
    var detectableObjectType = CommunityDetectableObjectType.builder().type(PASSAGE_PIETON).build();
    return CommunityAuthorization.builder()
        .detectableObjectTypes(List.of(detectableObjectType))
        .build();
  }
}
