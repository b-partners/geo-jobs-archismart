package app.bpartners.geojobs.service.dashboard;

import app.bpartners.geojobs.model.exception.DashboardUnavailableException;
import app.bpartners.geojobs.model.exception.PaymentRequiredException;
import app.bpartners.geojobs.repository.CommunityAuthorizationRepository;
import app.bpartners.geojobs.repository.model.community.CommunityAuthorization;
import java.util.function.Consumer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;

@Slf4j
@Component
@RequiredArgsConstructor
public class AnalysisProjectBilling {
  private final AnalysisProjectApi analysisProjectApi;
  private final CommunityAuthorizationRepository communityAuthorizationRepository;

  public static boolean isBilled(CommunityAuthorization community) {
    return community.getDashboardApiKey() != null && !community.getDashboardApiKey().isBlank();
  }

  public boolean register(
      CommunityAuthorization community, String subject, Consumer<String> registration) {
    if (!isBilled(community)) {
      return false;
    }
    try {
      registration.accept(community.getDashboardApiKey());
    } catch (HttpClientErrorException e) {
      if (!HttpStatus.PAYMENT_REQUIRED.equals(e.getStatusCode())) {
        throw e;
      }
      log.error("{} delivered but not billed, insufficient credits on dashboard", subject);
      return false;
    } catch (RestClientException e) {
      throw new DashboardUnavailableException(e);
    }
    return true;
  }

  public void checkCredits(
      String communityOwnerId, String projectKey, String subject, String action) {
    if (communityOwnerId == null) {
      return;
    }
    var community = communityAuthorizationRepository.findById(communityOwnerId).orElse(null);
    if (community == null || !isBilled(community)) {
      return;
    }

    var project = retrieveProject(community, projectKey, subject);
    if (project == null) {
      return;
    }
    if (project.creditsSufficient() == null) {
      log.warn("Dashboard gave no credit balance for the project of {}", subject);
      return;
    }
    if (!project.creditsSufficient()) {
      throw new PaymentRequiredException(
          "Not enough analysis credits to "
              + action
              + ", "
              + project.creditCost()
              + " credit(s) required. Please, buy credits on dashboard to access to this API.");
    }
  }

  private app.bpartners.geojobs.service.dashboard.component.AnalysisProject retrieveProject(
      CommunityAuthorization community, String projectKey, String subject) {
    try {
      return analysisProjectApi.get(community.getDashboardApiKey(), projectKey);
    } catch (RuntimeException e) {
      log.warn("Unable to check credits before {}", subject, e);
      return null;
    }
  }
}
