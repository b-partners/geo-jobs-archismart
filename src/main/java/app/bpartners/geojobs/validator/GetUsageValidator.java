package app.bpartners.geojobs.validator;

import app.bpartners.geojobs.endpoint.rest.security.model.Principal;
import app.bpartners.geojobs.model.exception.BadRequestException;
import java.util.function.Consumer;
import org.springframework.stereotype.Component;

@Component
public class GetUsageValidator implements Consumer<Principal> {
  @Override
  public void accept(Principal principal) {
    if (principal.isAdmin()) {
      throw new BadRequestException("Get usage is only for COMMUNITY_ROLE");
    }
  }
}
