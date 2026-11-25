package app.bpartners.geojobs.endpoint.rest.controller.v1;

import app.bpartners.geojobs.endpoint.rest.V1RestController;
import app.bpartners.geojobs.endpoint.rest.controller.v1.mapper.DashboardRegistrationFailureRestMapper;
import app.bpartners.geojobs.endpoint.rest.model.DashboardRegistrationFailure;
import app.bpartners.geojobs.service.DashboardRegistrationFailureService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;

@V1RestController
@RequiredArgsConstructor
public class DashboardRegistrationFailureController {
  private final DashboardRegistrationFailureService dashboardRegistrationFailureService;
  private final DashboardRegistrationFailureRestMapper mapper;

  @PutMapping("/dashboardRegistrationFailures/{id}/reprocess")
  public DashboardRegistrationFailure reprocessDashboardRegistrationFailure(
      @PathVariable String id) {
    return mapper.toRest(dashboardRegistrationFailureService.reprocess(id));
  }
}
