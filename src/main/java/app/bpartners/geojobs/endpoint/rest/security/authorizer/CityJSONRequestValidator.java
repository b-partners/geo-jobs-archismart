package app.bpartners.geojobs.endpoint.rest.security.authorizer;

import app.bpartners.geojobs.model.exception.BadRequestException;
import app.bpartners.geojobs.repository.CityJSONRequestRepository;
import java.util.function.BiConsumer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CityJSONRequestValidator implements BiConsumer<String, String> {
  private final CityJSONRequestRepository cityJSONRequestRepository;

  @Override
  public void accept(String requestId, String communityOwnerId) {

    var optionalRequest =
        cityJSONRequestRepository.findByIdAndCommunityOwnerId(requestId, communityOwnerId);
    if (optionalRequest.isEmpty()) {
      return;
    }

    throw new BadRequestException(
        "Process request with id " + requestId + " can not be either updated or processed again");
  }
}
