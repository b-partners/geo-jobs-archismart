package app.bpartners.geojobs.endpoint.rest.security.authorizer;

import static app.bpartners.geojobs.service.dashboard.DashboardUserStatus.*;
import static java.lang.System.currentTimeMillis;
import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import app.bpartners.geojobs.model.exception.ForbiddenException;
import app.bpartners.geojobs.repository.model.community.CommunityAuthorization;
import app.bpartners.geojobs.service.dashboard.DashboardUser;
import app.bpartners.geojobs.service.dashboard.DashboardUserSubscription;
import app.bpartners.geojobs.service.dashboard.SecurityApi;
import org.junit.jupiter.api.Test;

class CommunityUserSubscriptionAuthorizerTest {
  SecurityApi securityApiMock = mock();
  CommunityUserSubscriptionAuthorizer subject =
      new CommunityUserSubscriptionAuthorizer(securityApiMock);

  @Test
  void throws_exception_when_associated_dashboard_user_does_not_have_payment_method() {
    var userApiKey = randomUUID().toString();
    var communityAuthorization = mock(CommunityAuthorization.class);
    var dashboardUserMock = mock(DashboardUser.class);
    var communityEmail = "random.email." + currentTimeMillis() + "@mail.com";
    when(communityAuthorization.getEmail()).thenReturn(communityEmail);
    when(communityAuthorization.getDashboardApiKey()).thenReturn(userApiKey);
    when(dashboardUserMock.subscription())
        .thenReturn(new DashboardUserSubscription(PAYMENT_METHOD_REQUIRED, null, null));
    when(communityAuthorization.getApiKey()).thenReturn(userApiKey);
    when(securityApiMock.retrieveDashboardUserByApiKey(userApiKey)).thenReturn(dashboardUserMock);

    var actual =
        assertThrows(ForbiddenException.class, () -> subject.accept(communityAuthorization));

    assertEquals(
        "User.email="
            + communityEmail
            + " has no payment method on dashboard. Please, add it to access to this API.",
        actual.getMessage());
  }

  @Test
  void throws_exception_when_associated_dashboard_user_has_unpaid_subscription() {
    var userApiKey = randomUUID().toString();
    var communityAuthorization = mock(CommunityAuthorization.class);
    var dashboardUserMock = mock(DashboardUser.class);
    var communityEmail = "random.email." + currentTimeMillis() + "@mail.com";
    when(communityAuthorization.getEmail()).thenReturn(communityEmail);
    when(communityAuthorization.getDashboardApiKey()).thenReturn(userApiKey);
    when(dashboardUserMock.subscription())
        .thenReturn(new DashboardUserSubscription(UNPAID, null, null));
    when(communityAuthorization.getApiKey()).thenReturn(userApiKey);
    when(securityApiMock.retrieveDashboardUserByApiKey(userApiKey)).thenReturn(dashboardUserMock);

    var actual =
        assertThrows(ForbiddenException.class, () -> subject.accept(communityAuthorization));

    assertEquals(
        "User.email="
            + communityEmail
            + " has an irregular financial status on dashboard. Please, resolve it to access to"
            + " this API.",
        actual.getMessage());
  }

  @Test
  void throws_exception_when_associated_dashboard_user_has_empty_subscription() {
    var userApiKey = randomUUID().toString();
    var communityAuthorization = mock(CommunityAuthorization.class);
    var dashboardUserMock = mock(DashboardUser.class);
    var communityEmail = "random.email." + currentTimeMillis() + "@mail.com";
    when(communityAuthorization.getEmail()).thenReturn(communityEmail);
    when(communityAuthorization.getDashboardApiKey()).thenReturn(userApiKey);
    when(dashboardUserMock.subscription())
        .thenReturn(new DashboardUserSubscription(EMPTY, null, null));
    when(communityAuthorization.getApiKey()).thenReturn(userApiKey);
    when(securityApiMock.retrieveDashboardUserByApiKey(userApiKey)).thenReturn(dashboardUserMock);

    var actual =
        assertThrows(ForbiddenException.class, () -> subject.accept(communityAuthorization));

    assertEquals(
        "User.email="
            + communityEmail
            + " does not have valid subscription on dashboard. Please, resolve it to access to this"
            + " API.",
        actual.getMessage());
  }
}
