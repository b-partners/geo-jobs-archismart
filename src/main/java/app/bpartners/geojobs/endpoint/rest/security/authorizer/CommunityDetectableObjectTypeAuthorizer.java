package app.bpartners.geojobs.endpoint.rest.security.authorizer;

import app.bpartners.geojobs.endpoint.rest.controller.mapper.DetectableObjectTypeMapper;
import app.bpartners.geojobs.endpoint.rest.model.DetectableObjectType;
import app.bpartners.geojobs.model.exception.ForbiddenException;
import app.bpartners.geojobs.repository.model.community.CommunityAuthorization;
import app.bpartners.geojobs.repository.model.community.CommunityDetectableObjectType;
import java.util.function.BiConsumer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CommunityDetectableObjectTypeAuthorizer
    implements BiConsumer<CommunityAuthorization, DetectableObjectType> {
  private final DetectableObjectTypeMapper detectableObjectTypeMapper;

  @Override
  public void accept(
      CommunityAuthorization communityAuthorization, DetectableObjectType candidateObjectType) {
    var authorizedObjectTypes =
        communityAuthorization.getDetectableObjectTypes().stream()
            .map(CommunityDetectableObjectType::getType)
            .map(detectableObjectTypeMapper::toRest)
            .toList();

    if (!authorizedObjectTypes.contains(candidateObjectType)) {
      throw new ForbiddenException(
          "The following objects are not authorized for your community.name = "
              + communityAuthorization.getName()
              + " : "
              + candidateObjectType);
    }
  }
}
