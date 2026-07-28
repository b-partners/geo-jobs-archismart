package app.bpartners.geojobs.endpoint.rest.controller.mapper;

import app.bpartners.geojobs.endpoint.rest.model.RevokedApiKey;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class RevokedApiKeyMapper {

  public RevokedApiKey toRest(
      app.bpartners.geojobs.repository.model.community.RevokedApiKey domain) {
    return new RevokedApiKey()
        .revokedAt(domain.getRevokedAt())
        .keyValue(UUID.fromString(domain.getRevokedApiKeyValue()));
  }
}
