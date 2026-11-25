package app.bpartners.geojobs.service;

import app.bpartners.geojobs.repository.model.Feature;
import app.bpartners.geojobs.service.dashboard.AnalysisProjectBilling;
import app.bpartners.geojobs.service.dashboard.AnalysisProjectKeys;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DetectionCreditGuard {
  private final AnalysisProjectBilling analysisProjectBilling;

  public void accept(
      String communityOwnerId,
      String detectionIdentifier,
      String providedProjectKey,
      List<Feature> providedFeatures) {
    var individual = AnalysisProjectKeys.isIndividual(providedFeatures);
    var projectKey = AnalysisProjectKeys.providedOrNull(providedProjectKey);
    analysisProjectBilling.checkCredits(
        communityOwnerId,
        projectKey != null
            ? projectKey
            : projectKeyOf(detectionIdentifier, providedFeatures, individual),
        "detection " + detectionIdentifier,
        "run this detection");
  }

  private static String projectKeyOf(
      String detectionIdentifier, List<Feature> providedFeatures, boolean individual) {
    var detectionKey = "detection:" + detectionIdentifier;
    if (!individual) {
      return detectionKey;
    }
    return AnalysisProjectKeys.of(providedFeatures).orElse(detectionKey);
  }
}
