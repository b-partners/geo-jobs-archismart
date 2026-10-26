package app.bpartners.geojobs.endpoint.rest.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import app.bpartners.geojobs.endpoint.rest.controller.v1.CoverageController;
import app.bpartners.geojobs.endpoint.rest.controller.v1.mapper.CoverageRestMapper;
import app.bpartners.geojobs.endpoint.rest.model.ModelName;
import app.bpartners.geojobs.endpoint.rest.security.authorizer.CommunityModelCoverageFilter;
import app.bpartners.geojobs.model.exception.BadRequestException;
import app.bpartners.geojobs.service.coverage.AreaCoverageService;
import app.bpartners.geojobs.service.coverage.Coverage2D;
import app.bpartners.geojobs.service.coverage.Coverage3D;
import app.bpartners.geojobs.service.coverage.CoverageLocation;
import app.bpartners.geojobs.service.coverage.CoverageLocationResolver;
import app.bpartners.geojobs.service.coverage.CoverageType;
import app.bpartners.geojobs.service.coverage.ImageryLayer;
import app.bpartners.geojobs.service.coverage.LocationCoverage;
import java.util.List;
import java.util.Set;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class CoverageControllerTest {
  static final CoverageLocation LOCATION = new CoverageLocation(2.35, 48.85);
  CoverageLocationResolver locationResolverMock = mock();
  AreaCoverageService serviceMock = mock();
  CommunityModelCoverageFilter modelCoverageFilterMock = mock();
  CoverageController subject =
      new CoverageController(
          locationResolverMock, serviceMock, new CoverageRestMapper(), modelCoverageFilterMock);
  MockMvc mockMvc;

  @BeforeEach
  void setUp() {
    when(modelCoverageFilterMock.restrictToAuthorizedModels(any()))
        .thenAnswer(invocation -> invocation.getArgument(0));
    mockMvc = MockMvcBuilders.standaloneSetup(subject).build();
  }

  @SneakyThrows
  @Test
  void get_coverage_ok() {
    var imagery =
        new Coverage2D(
            true,
            true,
            new ImageryLayer("layer", 2024, "GEOSERVER", 5),
            List.of(ModelName.TOITURE, ModelName.REVETEMENT_OBSTACLE));
    var lidar = new Coverage3D(true, "FRANCE", "EPSG:2154", 2, 1, 1);
    when(locationResolverMock.resolve(null, 2.35, 48.85)).thenReturn(LOCATION);
    when(serviceMock.check(LOCATION, Set.of()))
        .thenReturn(new LocationCoverage(LOCATION, imagery, lidar));

    mockMvc
        .perform(get("/coverage").param("longitude", "2.35").param("latitude", "48.85"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.position.longitude").value(2.35))
        .andExpect(jsonPath("$.position.latitude").value(48.85))
        .andExpect(jsonPath("$.imagery.covered").value(true))
        .andExpect(jsonPath("$.imagery.hd").value(true))
        .andExpect(jsonPath("$.imagery.layer.name").value("layer"))
        .andExpect(jsonPath("$.imagery.layer.precisionInCm").value(5))
        .andExpect(jsonPath("$.imagery.supportedDetectableModels.length()").value(2))
        .andExpect(jsonPath("$.imagery.supportedDetectableModels[0].modelName").value("BP_TOITURE"))
        .andExpect(jsonPath("$.lidar.covered").value(true))
        .andExpect(jsonPath("$.lidar.region").value("FRANCE"))
        .andExpect(jsonPath("$.lidar.fileCount").value(2));
  }

  @SneakyThrows
  @Test
  void get_coverage_by_address_resolves_location_from_address() {
    when(locationResolverMock.resolve("paris", null, null)).thenReturn(LOCATION);
    when(serviceMock.check(LOCATION, Set.of()))
        .thenReturn(new LocationCoverage(LOCATION, Coverage2D.notCovered(), null));

    mockMvc
        .perform(get("/coverage").param("address", "paris"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.position.longitude").value(2.35))
        .andExpect(jsonPath("$.imagery.covered").value(false))
        .andExpect(jsonPath("$.imagery.layer").doesNotExist())
        .andExpect(jsonPath("$.lidar").doesNotExist());
  }

  @SneakyThrows
  @Test
  void get_coverage_parses_comma_separated_types() {
    when(locationResolverMock.resolve(null, 2.35, 48.85)).thenReturn(LOCATION);
    when(serviceMock.check(LOCATION, Set.of(CoverageType.IMAGERY, CoverageType.LIDAR)))
        .thenReturn(new LocationCoverage(LOCATION, null, null));

    mockMvc
        .perform(
            get("/coverage")
                .param("longitude", "2.35")
                .param("latitude", "48.85")
                .param("types", "LIDAR,IMAGERY"))
        .andExpect(status().isOk());

    verify(serviceMock).check(LOCATION, Set.of(CoverageType.IMAGERY, CoverageType.LIDAR));
  }

  @SneakyThrows
  @Test
  void get_coverage_is_served_on_both_default_and_v1_paths() {
    when(locationResolverMock.resolve(null, 2.35, 48.85)).thenReturn(LOCATION);
    when(serviceMock.check(LOCATION, Set.of()))
        .thenReturn(new LocationCoverage(LOCATION, Coverage2D.notCovered(), null));

    for (var path : List.of("/coverage", "/v1/coverage")) {
      mockMvc
          .perform(get(path).param("longitude", "2.35").param("latitude", "48.85"))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.imagery.covered").value(false));
    }
  }

  @Test
  void get_coverage_propagates_invalid_location() {
    when(locationResolverMock.resolve(null, null, null)).thenThrow(new BadRequestException("bad"));

    org.junit.jupiter.api.Assertions.assertThrows(
        BadRequestException.class, () -> subject.getAreaCoverage(null, null, null, null));
  }
}
