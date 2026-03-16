package app.bpartners.geojobs.endpoint.rest.security.authorizer;

import static app.bpartners.geojobs.service.dashboard.DashboardUserStatus.*;

import app.bpartners.geojobs.model.exception.ForbiddenException;
import app.bpartners.geojobs.repository.model.community.CommunityAuthorization;
import app.bpartners.geojobs.service.dashboard.SecurityApi;
import java.util.Objects;
import java.util.function.Consumer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CommunityUserSubscriptionAuthorizer implements Consumer<CommunityAuthorization> {
  private final SecurityApi securityApi;

  @Override
  public void accept(CommunityAuthorization authenticatedCommunity) {
    if (authenticatedCommunity.getDashboardApiKey() != null) {
      var dashboardUser =
          securityApi.retrieveDashboardUserByApiKey(authenticatedCommunity.getDashboardApiKey());
      var status = dashboardUser.subscription().status();
      if (Objects.equals(PAYMENT_METHOD_REQUIRED, status)) {
        throw new ForbiddenException(
            "User.email="
                + authenticatedCommunity.getEmail()
                + " has no payment method on dashboard. Please, add it to access to this API.");
      }
      if (Objects.equals(UNPAID, status)) {
        throw new ForbiddenException(
            "User.email="
                + authenticatedCommunity.getEmail()
                + " has an irregular financial status on dashboard. Please, resolve it to access to"
                + " this API.");
      }
      if (Objects.equals(EMPTY, status)) {
        throw new ForbiddenException(
            "User.email="
                + authenticatedCommunity.getEmail()
                + " does not have valid subscription on dashboard. Please, resolve it to access to"
                + " this API.");
      }
    }
  }
}
