package app.bpartners.geojobs.endpoint.rest.security.authorizer;

import app.bpartners.geojobs.endpoint.rest.model.ModelName;
import app.bpartners.geojobs.model.exception.ForbiddenException;
import app.bpartners.geojobs.repository.model.community.CommunityAuthorization;
import java.util.function.BiConsumer;
import org.springframework.stereotype.Component;

@Component
public class CommunityDetectableModelAuthorizer
    implements BiConsumer<CommunityAuthorization, ModelName> {

  @Override
  public void accept(CommunityAuthorization communityAuthorization, ModelName candidateModel) {
    if (!communityAuthorization.getDetectableModels().contains(candidateModel)) {
      throw new ForbiddenException(
          "The following models are not authorized for your community.name = "
              + communityAuthorization.getName()
              + " : "
              + candidateModel);
    }
  }
}
