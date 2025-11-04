package app.bpartners.geojobs.endpoint.rest.validator;

import static app.bpartners.geojobs.endpoint.rest.security.model.Authority.Role.ROLE_ADMIN;
import static app.bpartners.geojobs.endpoint.rest.security.model.Authority.Role.ROLE_COMMUNITY;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import app.bpartners.geojobs.endpoint.rest.security.model.Authority;
import app.bpartners.geojobs.endpoint.rest.security.model.Principal;
import app.bpartners.geojobs.model.exception.BadRequestException;
import java.util.Set;
import org.junit.jupiter.api.Test;

class GetUsageValidatorTest {
  GetUsageValidator subject = new GetUsageValidator();

  @Test
  void should_accept_community_role() {
    var communityPrincipal = new Principal("dummyApiKey", Set.of(new Authority(ROLE_COMMUNITY)));
    assertDoesNotThrow(() -> subject.accept(communityPrincipal));
  }

  @Test
  void should_not_accept_admin() {
    var adminPrincipal = new Principal("dummyApiKey", Set.of(new Authority(ROLE_ADMIN)));
    assertThrows(BadRequestException.class, () -> subject.accept(adminPrincipal));
  }
}
