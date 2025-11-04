package app.bpartners.geojobs.endpoint.rest.security.authorizer;

import app.bpartners.geojobs.model.exception.ForbiddenException;
import app.bpartners.geojobs.repository.model.community.CommunityAuthorization;
import app.bpartners.geojobs.repository.model.detection.Detection;
import java.util.function.BiConsumer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DetectionOwnerAuthorizer implements BiConsumer<CommunityAuthorization, Detection> {
  @Override
  public void accept(CommunityAuthorization communityAuthorization, Detection detection) {
    if (!communityAuthorization.getId().equals(detection.getCommunityOwnerId())) {
      throw new ForbiddenException(
          "Given endToEndId is not authorized for your community.name = "
              + detection.getCommunityOwnerId()
              + ", endToEndId = "
              + detection.getEndToEndId());
    }
  }
}
