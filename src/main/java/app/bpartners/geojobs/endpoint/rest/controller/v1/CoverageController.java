package app.bpartners.geojobs.endpoint.rest.controller.v1;

import app.bpartners.geojobs.endpoint.rest.V1RestController;
import app.bpartners.geojobs.endpoint.rest.controller.v1.mapper.CoverageRestMapper;
import app.bpartners.geojobs.endpoint.rest.model.AreaCoverage;
import app.bpartners.geojobs.endpoint.rest.model.CoverageType;
import app.bpartners.geojobs.endpoint.rest.security.authorizer.CommunityModelCoverageFilter;
import app.bpartners.geojobs.service.coverage.AreaCoverageService;
import app.bpartners.geojobs.service.coverage.CoverageLocationResolver;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@V1RestController
@RequiredArgsConstructor
public class CoverageController {
  private final CoverageLocationResolver locationResolver;
  private final AreaCoverageService service;
  private final CoverageRestMapper mapper;
  private final CommunityModelCoverageFilter modelCoverageFilter;

  @GetMapping("/coverage")
  public AreaCoverage getAreaCoverage(
      @RequestParam(value = "address", required = false) String address,
      @RequestParam(value = "longitude", required = false) Double longitude,
      @RequestParam(value = "latitude", required = false) Double latitude,
      @RequestParam(value = "types", required = false) List<CoverageType> types) {
    var location = locationResolver.resolve(address, longitude, latitude);
    var coverage = service.check(location, mapper.toDomain(types));
    return mapper.toRest(modelCoverageFilter.restrictToAuthorizedModels(coverage));
  }
}
