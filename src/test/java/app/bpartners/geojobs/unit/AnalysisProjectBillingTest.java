package app.bpartners.geojobs.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import app.bpartners.geojobs.model.exception.DashboardUnavailableException;
import app.bpartners.geojobs.repository.CommunityAuthorizationRepository;
import app.bpartners.geojobs.repository.model.community.CommunityAuthorization;
import app.bpartners.geojobs.service.dashboard.AnalysisProjectApi;
import app.bpartners.geojobs.service.dashboard.AnalysisProjectBilling;
import java.util.ArrayList;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;

class AnalysisProjectBillingTest {
  AnalysisProjectBilling subject =
      new AnalysisProjectBilling(
          mock(AnalysisProjectApi.class), mock(CommunityAuthorizationRepository.class));
  final CommunityAuthorization billed =
      CommunityAuthorization.builder().dashboardApiKey("dashboardApiKey").build();

  @Test
  void register_gives_the_dashboard_api_key_to_the_registration() {
    var apiKeys = new ArrayList<String>();

    var actual = subject.register(billed, "subject", apiKeys::add);

    assertTrue(actual);
    assertEquals(java.util.List.of("dashboardApiKey"), apiKeys);
  }

  @Test
  void register_is_skipped_for_a_community_not_billed() {
    var actual =
        subject.register(
            CommunityAuthorization.builder().build(),
            "subject",
            apiKey -> {
              throw new IllegalStateException("must not be called");
            });

    assertFalse(actual);
  }

  @Test
  void register_swallows_insufficient_credits() {
    var actual =
        subject.register(
            billed,
            "subject",
            apiKey -> {
              throw clientError(HttpStatus.PAYMENT_REQUIRED);
            });

    assertFalse(actual);
  }

  @Test
  void register_rethrows_other_client_errors() {
    assertThrows(
        HttpClientErrorException.class,
        () ->
            subject.register(
                billed,
                "subject",
                apiKey -> {
                  throw clientError(HttpStatus.FORBIDDEN);
                }));
  }

  @Test
  void register_signals_an_unreachable_dashboard() {
    assertThrows(
        DashboardUnavailableException.class,
        () ->
            subject.register(
                billed,
                "subject",
                apiKey -> {
                  throw new ResourceAccessException("connection refused");
                }));
  }

  @Test
  void register_signals_a_dashboard_server_error() {
    assertThrows(
        DashboardUnavailableException.class,
        () ->
            subject.register(
                billed,
                "subject",
                apiKey -> {
                  throw HttpServerErrorException.create(
                      HttpStatus.SERVICE_UNAVAILABLE,
                      "error",
                      HttpHeaders.EMPTY,
                      new byte[0],
                      null);
                }));
  }

  @Test
  void register_signals_an_unreadable_dashboard_response() {
    assertThrows(
        DashboardUnavailableException.class,
        () ->
            subject.register(
                billed,
                "subject",
                apiKey -> {
                  throw new RestClientException(
                      "no suitable HttpMessageConverter found for content type [text/html]");
                }));
  }

  private static HttpClientErrorException clientError(HttpStatus status) {
    return HttpClientErrorException.create(status, "error", HttpHeaders.EMPTY, new byte[0], null);
  }
}
