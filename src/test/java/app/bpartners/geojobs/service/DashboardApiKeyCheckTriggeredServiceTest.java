package app.bpartners.geojobs.service;

import static app.bpartners.geojobs.service.dashboard.component.UserApiKeyType.ANALYSIS;
import static app.bpartners.geojobs.service.dashboard.component.UserApiKeyType.DASHBOARD;
import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import app.bpartners.geojobs.endpoint.event.model.DashboardApiKeyCheckTriggered;
import app.bpartners.geojobs.mail.Email;
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
import org.mockito.ArgumentCaptor;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.web.client.RestClientResponseException;

class DashboardApiKeyCheckTriggeredServiceTest {

  UserAccountsApi userAccountsApiMock = mock(UserAccountsApi.class);
  Mailer mailerMock = mock(Mailer.class);
  HTMLTemplateParser htmlTemplateParser = new HTMLTemplateParser();
  DashboardApiKeyCheckTriggeredService subject;

  @Test
  void log_when_check_on_not_existing_user() {
    CommunityAuthorization authorizationMock = mock();
    String adminApiKey = randomUUID().toString();

    when(authorizationMock.getEmail()).thenReturn("non-existant-email");
    when(userAccountsApiMock.getUsersByCriteria(
            eq(authorizationMock.getEmail()), eq(null), eq(null), anyString()))
        .thenReturn(List.of());

    DashboardApiKeyCheckTriggered event =
        DashboardApiKeyCheckTriggered.builder().email(authorizationMock.getEmail()).build();
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
      verifyNoInteractions(mailerMock);

      boolean hasExpectedInfoLog =
          appender.list.stream()
              .anyMatch(
                  e ->
                      e.getLevel() == Level.INFO
                          && e.getFormattedMessage()
                              .equals(
                                  "Users with email "
                                      + authorizationMock.getEmail()
                                      + " not found in user accounts api."));
      assertTrue(hasExpectedInfoLog);
    } finally {
      logger.detachAppender(appender);
      appender.stop();
    }
  }

  @Test
  void no_keys_found_on_existing_user_notifies_by_email() {
    CommunityAuthorization authorization = mock();
    User user =
        new User(
            randomUUID().toString(),
            randomUUID().toString(),
            randomUUID().toString(),
            "existant@mail.com");
    String adminApiKey = randomUUID().toString();

    when(authorization.getEmail()).thenReturn("existant-email");
    when(userAccountsApiMock.getUsersByCriteria(
            eq(authorization.getEmail()), eq(null), eq(null), anyString()))
        .thenReturn(List.of(user));
    when(userAccountsApiMock.getUserApiKey(eq(user.id()), eq(adminApiKey))).thenReturn(List.of());

    DashboardApiKeyCheckTriggered event =
        DashboardApiKeyCheckTriggered.builder().email(authorization.getEmail()).build();
    subject =
        new DashboardApiKeyCheckTriggeredService(
            userAccountsApiMock, adminApiKey, mailerMock, htmlTemplateParser);

    assertDoesNotThrow(() -> subject.accept(event));

    verify(userAccountsApiMock, times(1))
        .getUsersByCriteria(eq(authorization.getEmail()), eq(null), eq(null), eq(adminApiKey));
    verify(userAccountsApiMock, times(1)).getUserApiKey(eq(user.id()), eq(adminApiKey));

    ArgumentCaptor<Email> emailCaptor = ArgumentCaptor.forClass(Email.class);
    verify(mailerMock, times(1)).accept(emailCaptor.capture());

    Email sent = emailCaptor.getValue();
    assertNotNull(sent);
    assertTrue(sent.htmlBody().contains("No api found for users with email"));
    assertTrue(sent.htmlBody().contains(user.email()));
  }

  @Test
  void no_dashboard_keys_found_on_existing_user_notifies_by_email() {
    String adminApiKey = randomUUID().toString();
    String existingEmail = "exist@" + randomUUID();

    CommunityAuthorization authorization = mock();
    User user =
        new User(
            randomUUID().toString(),
            randomUUID().toString(),
            randomUUID().toString(),
            existingEmail);

    when(authorization.getEmail()).thenReturn(existingEmail);
    when(userAccountsApiMock.getUsersByCriteria(
            eq(authorization.getEmail()), eq(null), eq(null), anyString()))
        .thenReturn(List.of(user));
    when(userAccountsApiMock.getUserApiKey(eq(user.id()), eq(adminApiKey)))
        .thenReturn(List.of(new UserApiKey(randomUUID().toString(), ANALYSIS)));

    DashboardApiKeyCheckTriggered event =
        DashboardApiKeyCheckTriggered.builder().email(authorization.getEmail()).build();
    subject =
        new DashboardApiKeyCheckTriggeredService(
            userAccountsApiMock, adminApiKey, mailerMock, htmlTemplateParser);

    assertDoesNotThrow(() -> subject.accept(event));

    verify(userAccountsApiMock, times(1))
        .getUsersByCriteria(eq(authorization.getEmail()), eq(null), eq(null), eq(adminApiKey));
    verify(userAccountsApiMock, times(1)).getUserApiKey(eq(user.id()), eq(adminApiKey));

    ArgumentCaptor<Email> emailCaptor = ArgumentCaptor.forClass(Email.class);
    verify(mailerMock, times(1)).accept(emailCaptor.capture());

    Email sent = emailCaptor.getValue();
    assertNotNull(sent);
    assertTrue(sent.htmlBody().contains("No dashboard api key found for users with email"));
    assertTrue(sent.htmlBody().contains(existingEmail));
  }

  @Test
  void no_dashboard_keys_matches_actual_keys_notifies_by_email() {
    String adminApiKey = randomUUID().toString();
    String actualDashboardApiKey = randomUUID().toString();
    String existingEmail = "exist@" + randomUUID();

    CommunityAuthorization authorization = mock();
    User user =
        new User(
            randomUUID().toString(),
            randomUUID().toString(),
            randomUUID().toString(),
            existingEmail);

    when(authorization.getEmail()).thenReturn(existingEmail);
    when(authorization.getDashboardApiKey()).thenReturn(actualDashboardApiKey);
    when(userAccountsApiMock.getUsersByCriteria(
            eq(authorization.getEmail()), eq(null), eq(null), anyString()))
        .thenReturn(List.of(user));
    when(userAccountsApiMock.getUserApiKey(eq(user.id()), eq(adminApiKey)))
        .thenReturn(List.of(new UserApiKey(randomUUID().toString(), DASHBOARD)));

    DashboardApiKeyCheckTriggered event =
        DashboardApiKeyCheckTriggered.builder()
            .email(authorization.getEmail())
            .dashboardApiKey(authorization.getDashboardApiKey())
            .build();
    subject =
        new DashboardApiKeyCheckTriggeredService(
            userAccountsApiMock, adminApiKey, mailerMock, htmlTemplateParser);

    assertDoesNotThrow(() -> subject.accept(event));

    verify(userAccountsApiMock, times(1))
        .getUsersByCriteria(eq(authorization.getEmail()), eq(null), eq(null), eq(adminApiKey));
    verify(userAccountsApiMock, times(1)).getUserApiKey(eq(user.id()), eq(adminApiKey));

    ArgumentCaptor<Email> emailCaptor = ArgumentCaptor.forClass(Email.class);
    verify(mailerMock, times(1)).accept(emailCaptor.capture());

    Email sent = emailCaptor.getValue();
    assertNotNull(sent);
    assertTrue(sent.htmlBody().contains(actualDashboardApiKey));
    assertTrue(sent.htmlBody().contains(existingEmail));
  }

  @Test
  void warn_when_multiple_users_found_for_same_email() {
    String adminApiKey = randomUUID().toString();
    String email = "exist@" + randomUUID();

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
        DashboardApiKeyCheckTriggered.builder().email(authorization.getEmail()).build();
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
                              .equals(
                                  "Multiple (2) account attached to the email : "
                                      + email
                                      + " in user account api"));
      assertTrue(hasExpectedWarnLog);
    } finally {
      logger.detachAppender(appender);
      appender.stop();
    }
  }

  @Test
  void warn_when_unable_to_get_api_key_for_a_user() {
    String adminApiKey = randomUUID().toString();
    String email = "exist@" + randomUUID();

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

    DashboardApiKeyCheckTriggered event =
        DashboardApiKeyCheckTriggered.builder().email(authorization.getEmail()).build();
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
                              .equals(
                                  "Unable to get api key for user with id : "
                                      + user.id()
                                      + " in user account api"));
      assertTrue(hasExpectedWarnLog);
    } finally {
      logger.detachAppender(appender);
      appender.stop();
    }
  }
}
