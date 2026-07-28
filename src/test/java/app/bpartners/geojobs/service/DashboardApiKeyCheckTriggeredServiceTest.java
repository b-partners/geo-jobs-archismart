package app.bpartners.geojobs.service;

import static app.bpartners.geojobs.service.dashboard.component.UserApiKeyType.DASHBOARD;
import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import app.bpartners.geojobs.endpoint.event.model.DashboardApiKeyCheckTriggered;
import app.bpartners.geojobs.mail.Mailer;
import app.bpartners.geojobs.repository.model.community.CommunityAuthorization;
import app.bpartners.geojobs.service.dashboard.UserAccountsApi;
import app.bpartners.geojobs.service.dashboard.component.User;
import app.bpartners.geojobs.service.dashboard.component.UserApiKey;
import app.bpartners.geojobs.service.event.DashboardApiKeyCheckTriggeredService;
import app.bpartners.geojobs.template.HTMLTemplateParser;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.web.client.RestClientResponseException;

class DashboardApiKeyCheckTriggeredServiceTest {

  UserAccountsApi userAccountsApiMock = mock(UserAccountsApi.class);
  Mailer mailerMock = mock(Mailer.class);
  HTMLTemplateParser htmlTemplateParser = mock(HTMLTemplateParser.class);
  DashboardApiKeyCheckTriggeredService subject;

  @Test
  void success_when_dashboard_key_matches() {
    String adminApiKey = randomUUID().toString();
    String actualDashboardApiKey = randomUUID().toString();
    String existingEmail = "exist@" + randomUUID();
    String authId = randomUUID().toString();

    User user =
        new User(
            randomUUID().toString(),
            randomUUID().toString(),
            randomUUID().toString(),
            existingEmail);

    when(userAccountsApiMock.getUsersByCriteria(eq(existingEmail), eq(null), eq(null), anyString()))
        .thenReturn(List.of(user));
    when(userAccountsApiMock.getUserApiKey(user.id(), adminApiKey))
        .thenReturn(List.of(new UserApiKey(actualDashboardApiKey, DASHBOARD)));

    DashboardApiKeyCheckTriggered event =
        DashboardApiKeyCheckTriggered.builder()
            .email(existingEmail)
            .dashboardApiKey(actualDashboardApiKey)
            .communityAuthorizationId(authId)
            .build();

    subject =
        new DashboardApiKeyCheckTriggeredService(
            userAccountsApiMock, adminApiKey, mailerMock, htmlTemplateParser);

    Logger logger = (Logger) LoggerFactory.getLogger(DashboardApiKeyCheckTriggeredService.class);
    ListAppender<ILoggingEvent> appender = new ListAppender<>();
    appender.start();
    logger.addAppender(appender);

    try {
      assertDoesNotThrow(() -> subject.accept(event));

      verify(userAccountsApiMock, times(1))
          .getUsersByCriteria(existingEmail, null, null, adminApiKey);
      verify(userAccountsApiMock, times(1)).getUserApiKey(user.id(), adminApiKey);
      verifyNoInteractions(mailerMock);

      boolean hasExpectedInfoLog =
          appender.list.stream()
              .anyMatch(
                  e ->
                      e.getLevel() == Level.INFO
                          && e.getFormattedMessage()
                              .equals(
                                  "[DAKC S] Dashboard api key verification for user "
                                      + authId
                                      + " succeeded."));
      assertTrue(hasExpectedInfoLog);
    } finally {
      logger.detachAppender(appender);
      appender.stop();
    }
  }

  @Test
  void log_when_check_on_not_existing_user() {
    CommunityAuthorization authorizationMock = mock();
    String adminApiKey = randomUUID().toString();
    String authId = randomUUID().toString();

    when(authorizationMock.getEmail()).thenReturn("non-existant-email");
    when(userAccountsApiMock.getUsersByCriteria(
            eq(authorizationMock.getEmail()), eq(null), eq(null), anyString()))
        .thenReturn(List.of());

    DashboardApiKeyCheckTriggered event =
        DashboardApiKeyCheckTriggered.builder()
            .email(authorizationMock.getEmail())
            .communityAuthorizationId(authId)
            .build();

    subject =
        new DashboardApiKeyCheckTriggeredService(
            userAccountsApiMock, adminApiKey, mailerMock, htmlTemplateParser);

    Logger logger = (Logger) LoggerFactory.getLogger(DashboardApiKeyCheckTriggeredService.class);
    ListAppender<ILoggingEvent> appender = new ListAppender<>();
    appender.start();
    logger.addAppender(appender);

    try {
      assertDoesNotThrow(() -> subject.accept(event));

      verify(userAccountsApiMock, times(1))
          .getUsersByCriteria(
              eq(authorizationMock.getEmail()), eq(null), eq(null), eq(adminApiKey));

      boolean hasExpectedErrorLog =
          appender.list.stream()
              .anyMatch(
                  e ->
                      e.getLevel() == Level.WARN
                          && e.getFormattedMessage()
                              .equals(
                                  "[DAKC F] No users with same email as "
                                      + authId
                                      + " found in user account api."));
      assertTrue(hasExpectedErrorLog);
    } finally {
      logger.detachAppender(appender);
      appender.stop();
    }
  }

  @Test
  void warn_when_multiple_users_found_for_same_email() {
    String adminApiKey = randomUUID().toString();
    String email = "exist@" + randomUUID();
    String authId = randomUUID().toString();

    CommunityAuthorization authorization = mock();
    when(authorization.getEmail()).thenReturn(email);

    User user1 =
        new User(randomUUID().toString(), randomUUID().toString(), randomUUID().toString(), email);
    User user2 =
        new User(randomUUID().toString(), randomUUID().toString(), randomUUID().toString(), email);

    when(userAccountsApiMock.getUsersByCriteria(eq(email), eq(null), eq(null), anyString()))
        .thenReturn(List.of(user1, user2));
    when(userAccountsApiMock.getUserApiKey(anyString(), eq(adminApiKey))).thenReturn(List.of());

    DashboardApiKeyCheckTriggered event =
        DashboardApiKeyCheckTriggered.builder()
            .email(authorization.getEmail())
            .communityAuthorizationId(authId)
            .build();

    subject =
        new DashboardApiKeyCheckTriggeredService(
            userAccountsApiMock, adminApiKey, mailerMock, htmlTemplateParser);

    Logger logger = (Logger) LoggerFactory.getLogger(DashboardApiKeyCheckTriggeredService.class);
    ListAppender<ILoggingEvent> appender = new ListAppender<>();
    appender.start();
    logger.addAppender(appender);

    try {
      assertDoesNotThrow(() -> subject.accept(event));

      boolean hasExpectedWarnLog =
          appender.list.stream()
              .anyMatch(
                  e ->
                      e.getLevel() == Level.WARN
                          && e.getFormattedMessage()
                              .contains(
                                  "Multiple (2) account ( "
                                      + user1.id()
                                      + " "
                                      + user2.id()
                                      + " ) attached to the email of the user "
                                      + authId));
      assertTrue(hasExpectedWarnLog);
    } finally {
      logger.detachAppender(appender);
      appender.stop();
    }
  }

  @Test
  void error_when_unable_to_get_api_key_for_a_user() {
    String adminApiKey = randomUUID().toString();
    String email = "exist@" + randomUUID();
    String authId = randomUUID().toString();

    CommunityAuthorization authorization = mock();
    when(authorization.getEmail()).thenReturn(email);

    User user =
        new User(randomUUID().toString(), randomUUID().toString(), randomUUID().toString(), email);

    when(userAccountsApiMock.getUsersByCriteria(eq(email), eq(null), eq(null), anyString()))
        .thenReturn(List.of(user));

    when(userAccountsApiMock.getUserApiKey(eq(user.id()), eq(adminApiKey)))
        .thenThrow(
            new RestClientResponseException(
                "Error", 500, "Internal Server Error", HttpHeaders.EMPTY, null, null));
    when(htmlTemplateParser.apply(anyString(), any())).thenReturn("Mock HTML Body Content");

    DashboardApiKeyCheckTriggered event =
        DashboardApiKeyCheckTriggered.builder()
            .email(authorization.getEmail())
            .communityAuthorizationId(authId)
            .build();

    subject =
        new DashboardApiKeyCheckTriggeredService(
            userAccountsApiMock, adminApiKey, mailerMock, htmlTemplateParser);

    Logger logger = (Logger) LoggerFactory.getLogger(DashboardApiKeyCheckTriggeredService.class);
    ListAppender<ILoggingEvent> appender = new ListAppender<>();
    appender.start();
    logger.addAppender(appender);

    try {
      assertDoesNotThrow(() -> subject.accept(event));

      boolean hasExpectedErrorLog =
          appender.list.stream()
              .anyMatch(
                  e ->
                      e.getLevel() == Level.ERROR
                          && e.getFormattedMessage()
                              .contains(
                                  "[DAKC F] Unable to get api key for user with id : "
                                      + user.id()
                                      + " in user account api."));
      assertTrue(hasExpectedErrorLog);
    } finally {
      logger.detachAppender(appender);
      appender.stop();
    }
  }
}
