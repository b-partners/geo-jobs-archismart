package app.bpartners.geojobs.service;

import static app.bpartners.geojobs.service.ThreeDTrackingRegister.projectKeyOf;

import app.bpartners.geojobs.repository.model.cityjson.CityJSONRequest;
import app.bpartners.geojobs.service.dashboard.AnalysisProjectBilling;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ThreeDCreditGuard {
  private final AnalysisProjectBilling analysisProjectBilling;

  public void accept(CityJSONRequest request) {
    analysisProjectBilling.checkCredits(
        request.getCommunityOwnerId(),
        projectKeyOf(request),
        "3D request " + request.getId(),
        "generate this 3D model");
  }
}
