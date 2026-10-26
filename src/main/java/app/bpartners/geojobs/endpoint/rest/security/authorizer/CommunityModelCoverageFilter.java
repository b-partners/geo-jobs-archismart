package app.bpartners.geojobs.endpoint.rest.security.authorizer;

import app.bpartners.geojobs.endpoint.rest.controller.v1.mapper.DetectableObjectTypeMapper;
import app.bpartners.geojobs.endpoint.rest.model.ModelName;
import app.bpartners.geojobs.endpoint.rest.security.AuthProvider;
import app.bpartners.geojobs.repository.CommunityAuthorizationRepository;
import app.bpartners.geojobs.repository.model.community.CommunityAuthorization;
import app.bpartners.geojobs.repository.model.community.CommunityDetectableObjectType;
import app.bpartners.geojobs.repository.model.detection.DetectableType;
import app.bpartners.geojobs.service.coverage.LocationCoverage;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CommunityModelCoverageFilter {
  private final AuthProvider authProvider;
  private final CommunityAuthorizationRepository communityAuthorizationRepository;
  private final DetectableObjectTypeMapper objectTypeMapper;

  public LocationCoverage restrictToAuthorizedModels(LocationCoverage coverage) {
    if (coverage.imagery() == null || authProvider.getPrincipal().isAdmin()) {
      return coverage;
    }
    var authorizedModels =
        communityAuthorizationRepository
            .findByApiKey(authProvider.getPrincipal().getPassword())
            .map(
                authorization ->
                    authorizedAmong(authorization, coverage.imagery().supportedModels()))
            .orElse(List.of());
    return new LocationCoverage(
        coverage.location(),
        coverage.imagery().withSupportedModels(authorizedModels),
        coverage.lidar());
  }

  private List<ModelName> authorizedAmong(
      CommunityAuthorization authorization, List<ModelName> models) {
    var authorizedModels = authorization.getDetectableModels();
    if (authorizedModels != null && !authorizedModels.isEmpty()) {
      return models.stream().filter(authorizedModels::contains).toList();
    }
    var authorizedTypes = authorizedTypesOf(authorization);
    return models.stream().filter(model -> authorizedTypes.containsAll(typesOf(model))).toList();
  }

  private Set<DetectableType> authorizedTypesOf(CommunityAuthorization authorization) {
    var types = authorization.getDetectableObjectTypes();
    var authorizedTypes = new HashSet<DetectableType>();
    if (types != null) {
      types.stream().map(CommunityDetectableObjectType::getType).forEach(authorizedTypes::add);
    }
    return authorizedTypes;
  }

  private Set<DetectableType> typesOf(ModelName model) {
    return new HashSet<>(
        objectTypeMapper.mapFromModel(model).stream().map(objectTypeMapper::toDomain).toList());
  }
}
