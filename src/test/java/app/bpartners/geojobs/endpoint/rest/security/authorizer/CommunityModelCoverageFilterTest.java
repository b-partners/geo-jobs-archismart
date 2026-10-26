package app.bpartners.geojobs.endpoint.rest.security.authorizer;

import static app.bpartners.geojobs.endpoint.rest.model.ModelName.CLIMAT_RESILIENCE;
import static app.bpartners.geojobs.endpoint.rest.model.ModelName.REVETEMENT_OBSTACLE;
import static app.bpartners.geojobs.endpoint.rest.model.ModelName.TOITURE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import app.bpartners.geojobs.endpoint.rest.controller.v1.mapper.DetectableObjectTypeMapper;
import app.bpartners.geojobs.endpoint.rest.model.ModelName;
import app.bpartners.geojobs.endpoint.rest.security.AuthProvider;
import app.bpartners.geojobs.endpoint.rest.security.model.Principal;
import app.bpartners.geojobs.repository.CommunityAuthorizationRepository;
import app.bpartners.geojobs.repository.model.community.CommunityAuthorization;
import app.bpartners.geojobs.repository.model.community.CommunityDetectableObjectType;
import app.bpartners.geojobs.repository.model.detection.DetectableType;
import app.bpartners.geojobs.service.coverage.Coverage2D;
import app.bpartners.geojobs.service.coverage.CoverageLocation;
import app.bpartners.geojobs.service.coverage.ImageryLayer;
import app.bpartners.geojobs.service.coverage.LocationCoverage;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

class CommunityModelCoverageFilterTest {
  static final String API_KEY = "key";
  static final List<ModelName> SUPPORTED = List.of(TOITURE, CLIMAT_RESILIENCE, REVETEMENT_OBSTACLE);
  AuthProvider authProviderMock = mock();
  CommunityAuthorizationRepository repositoryMock = mock();
  CommunityModelCoverageFilter subject =
      new CommunityModelCoverageFilter(
          authProviderMock, repositoryMock, new DetectableObjectTypeMapper());

  private static LocationCoverage coverage() {
    return new LocationCoverage(
        new CoverageLocation(2.35, 48.85),
        new Coverage2D(true, true, new ImageryLayer("layer", 2024, "GEOSERVER", 5), SUPPORTED),
        null);
  }

  private void authenticatedAs(String role) {
    when(authProviderMock.getPrincipal())
        .thenReturn(new Principal(API_KEY, List.of(new SimpleGrantedAuthority(role))));
  }

  private List<ModelName> restricted() {
    return subject.restrictToAuthorizedModels(coverage()).imagery().supportedModels();
  }

  @Test
  void admin_gets_every_supported_model() {
    authenticatedAs("ROLE_ADMIN");

    assertEquals(SUPPORTED, restricted());
  }

  @Test
  void community_gets_only_its_authorized_models() {
    authenticatedAs("ROLE_COMMUNITY");
    when(repositoryMock.findByApiKey(API_KEY))
        .thenReturn(
            Optional.of(
                CommunityAuthorization.builder().detectableModels(List.of(TOITURE)).build()));

    assertEquals(List.of(TOITURE), restricted());
  }

  @Test
  void community_without_models_gets_models_whose_every_type_is_authorized() {
    authenticatedAs("ROLE_COMMUNITY");
    var authorization =
        CommunityAuthorization.builder()
            .detectableObjectTypes(
                List.of(
                    CommunityDetectableObjectType.builder().type(DetectableType.PARKING).build(),
                    CommunityDetectableObjectType.builder()
                        .type(DetectableType.PANNEAU_PHOTOVOLTAIQUE)
                        .build()))
            .build();
    when(repositoryMock.findByApiKey(API_KEY)).thenReturn(Optional.of(authorization));

    assertEquals(List.of(CLIMAT_RESILIENCE), restricted());
  }

  @Test
  void unknown_community_gets_no_model() {
    authenticatedAs("ROLE_COMMUNITY");
    when(repositoryMock.findByApiKey(API_KEY)).thenReturn(Optional.empty());

    assertEquals(List.of(), restricted());
  }

  @Test
  void lidar_only_coverage_is_left_untouched() {
    var lidarOnly = new LocationCoverage(new CoverageLocation(2.35, 48.85), null, null);

    assertNull(subject.restrictToAuthorizedModels(lidarOnly).imagery());
  }
}
