package app.bpartners.geojobs.service;

import static app.bpartners.geojobs.service.dashboard.component.UserApiKeyType.DASHBOARD;
import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import app.bpartners.geojobs.endpoint.event.EventProducer;
import app.bpartners.geojobs.endpoint.event.model.DashboardApiKeyCheckTriggered;
import app.bpartners.geojobs.repository.CommunityAuthorizationRepository;
import app.bpartners.geojobs.repository.model.community.CommunityAuthorization;
import app.bpartners.geojobs.service.dashboard.UserAccountsApi;
import app.bpartners.geojobs.service.dashboard.component.User;
import app.bpartners.geojobs.service.dashboard.component.UserApiKey;
import app.bpartners.geojobs.service.event.DashboardApiKeyCheckTriggeredService;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

class DashboardApiKeyCheckTriggeredServiceTest {

  UserAccountsApi userAccountsApiMock = mock();
  CommunityAuthorizationRepository communityAuthorizationRepository = mock();
  EventProducer<DashboardApiKeyCheckTriggered> eventProducerMock = mock();
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
            userAccountsApiMock, adminApiKey, communityAuthorizationRepository, eventProducerMock);

    Logger logger = (Logger) LoggerFactory.getLogger(DashboardApiKeyCheckTriggeredService.class);
    ListAppender<ILoggingEvent> appender = new ListAppender<>();
    appender.start();
    logger.addAppender(appender);

    try {
      assertDoesNotThrow(() -> subject.accept(event));

      verify(userAccountsApiMock, times(1))
          .getUsersByCriteria(existingEmail, null, null, adminApiKey);
      verify(userAccountsApiMock, times(1)).getUserApiKey(user.id(), adminApiKey);

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
            userAccountsApiMock, adminApiKey, communityAuthorizationRepository, eventProducerMock);

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
    String actualDashboardApiKey = randomUUID().toString();
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
    when(userAccountsApiMock.getUserApiKey(anyString(), eq(adminApiKey)))
        .thenReturn(List.of(new UserApiKey(actualDashboardApiKey, DASHBOARD)));

    DashboardApiKeyCheckTriggered event =
        DashboardApiKeyCheckTriggered.builder()
            .email(authorization.getEmail())
            .dashboardApiKey(actualDashboardApiKey)
            .communityAuthorizationId(authId)
            .build();

    subject =
        new DashboardApiKeyCheckTriggeredService(
            userAccountsApiMock, adminApiKey, communityAuthorizationRepository, eventProducerMock);

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
  void log_handler_when_no_key_found_and_updated_successfully() {
    String adminApiKey = randomUUID().toString();
    String email = "exist@" + randomUUID();
    String authId = randomUUID().toString();

    CommunityAuthorization authorization = new CommunityAuthorization();
    when(communityAuthorizationRepository.findById(authId)).thenReturn(Optional.of(authorization));

    User user =
        new User(randomUUID().toString(), randomUUID().toString(), randomUUID().toString(), email);

    when(userAccountsApiMock.getUsersByCriteria(eq(email), eq(null), eq(null), anyString()))
        .thenReturn(List.of(user));
    when(userAccountsApiMock.getUserApiKey(user.id(), adminApiKey)).thenReturn(List.of());

    when(userAccountsApiMock.getOrGenerateApiKey(eq(email), anyString(), eq(adminApiKey)))
        .thenAnswer(invocation -> new UserApiKey(invocation.getArgument(1), DASHBOARD));

    DashboardApiKeyCheckTriggered event =
        DashboardApiKeyCheckTriggered.builder()
            .email(email)
            .communityAuthorizationId(authId)
            .build();

    subject =
        new DashboardApiKeyCheckTriggeredService(
            userAccountsApiMock, adminApiKey, communityAuthorizationRepository, eventProducerMock);

    Logger logger = (Logger) LoggerFactory.getLogger(DashboardApiKeyCheckTriggeredService.class);
    ListAppender<ILoggingEvent> appender = new ListAppender<>();
    appender.start();
    logger.addAppender(appender);

    try {
      assertDoesNotThrow(() -> subject.accept(event));

      boolean hasNewKeyGeneratedLog =
          appender.list.stream()
              .anyMatch(
                  e ->
                      e.getLevel() == Level.INFO
                          && e.getFormattedMessage()
                              .equals(
                                  "[DAKC H] New Dashboard api key generated for user " + authId));
      assertTrue(hasNewKeyGeneratedLog);

      boolean hasUpdatedLog =
          appender.list.stream()
              .anyMatch(
                  e ->
                      e.getLevel() == Level.INFO
                          && e.getFormattedMessage()
                              .equals("[DAKC H] Dashboard api key updated for user " + authId));
      assertTrue(hasUpdatedLog);
    } finally {
      logger.detachAppender(appender);
      appender.stop();
    }
  }

  @Test
  void log_handler_error_when_community_authorization_not_found() {
    String adminApiKey = randomUUID().toString();
    String email = "exist@" + randomUUID();
    String authId = randomUUID().toString();

    User user =
        new User(randomUUID().toString(), randomUUID().toString(), randomUUID().toString(), email);

    when(userAccountsApiMock.getUsersByCriteria(eq(email), eq(null), eq(null), anyString()))
        .thenReturn(List.of(user));
    when(userAccountsApiMock.getUserApiKey(user.id(), adminApiKey)).thenReturn(List.of());

    when(userAccountsApiMock.getOrGenerateApiKey(eq(email), anyString(), eq(adminApiKey)))
        .thenReturn(new UserApiKey("existingKey", DASHBOARD));

    when(communityAuthorizationRepository.findById(authId)).thenReturn(Optional.empty());

    DashboardApiKeyCheckTriggered event =
        DashboardApiKeyCheckTriggered.builder()
            .email(email)
            .communityAuthorizationId(authId)
            .build();

    subject =
        new DashboardApiKeyCheckTriggeredService(
            userAccountsApiMock, adminApiKey, communityAuthorizationRepository, eventProducerMock);

    Logger logger = (Logger) LoggerFactory.getLogger(DashboardApiKeyCheckTriggeredService.class);
    ListAppender<ILoggingEvent> appender = new ListAppender<>();
    appender.start();
    logger.addAppender(appender);

    try {
      assertDoesNotThrow(() -> subject.accept(event));

      boolean hasExistingKeyLog =
          appender.list.stream()
              .anyMatch(
                  e ->
                      e.getLevel() == Level.INFO
                          && e.getFormattedMessage()
                              .equals(
                                  "[DAKC H] Dashboard api key for user "
                                      + authId
                                      + " already exists in the user account api. No new key"
                                      + " generated."));
      assertTrue(hasExistingKeyLog);

      boolean hasUserNotFoundLog =
          appender.list.stream()
              .anyMatch(
                  e ->
                      e.getLevel() == Level.ERROR
                          && e.getFormattedMessage()
                              .equals(
                                  "[DAKC H] Failed to update dashboard api key for user "
                                      + authId
                                      + ". User not found in database."));
      assertTrue(hasUserNotFoundLog);
    } finally {
      logger.detachAppender(appender);
      appender.stop();
    }
  }

  @Test
  void accept_continues_lane_with_authorization_lanes_positions_later() {
    var ids = java.util.stream.IntStream.range(0, 25).mapToObj(i -> "id-" + i).toList();
    var next = mock(CommunityAuthorization.class);
    when(next.getId()).thenReturn("id-13");
    when(next.getEmail()).thenReturn("next@test.com");
    when(next.getDashboardApiKey()).thenReturn("next-key");
    when(communityAuthorizationRepository.findAllIdsByIntegrationTestUsageFalse()).thenReturn(ids);
    when(communityAuthorizationRepository.findById("id-13")).thenReturn(Optional.of(next));
    when(userAccountsApiMock.getUsersByCriteria(any(), any(), any(), any()))
        .thenThrow(new RuntimeException("429"));
    subject =
        new DashboardApiKeyCheckTriggeredService(
            userAccountsApiMock, "admin", communityAuthorizationRepository, eventProducerMock);

    subject.accept(
        DashboardApiKeyCheckTriggered.builder()
            .communityAuthorizationId("id-3")
            .email("a@test.com")
            .dashboardApiKey("key")
            .index(3)
            .build());

    verify(eventProducerMock)
        .accept(
            List.of(
                DashboardApiKeyCheckTriggered.builder()
                    .communityAuthorizationId("id-13")
                    .email("next@test.com")
                    .dashboardApiKey("next-key")
                    .index(13)
                    .build()));
  }

  @Test
  void accept_at_end_of_lane_sends_nothing() {
    when(communityAuthorizationRepository.findAllIdsByIntegrationTestUsageFalse())
        .thenReturn(List.of("a", "b"));
    when(userAccountsApiMock.getUsersByCriteria(any(), any(), any(), any())).thenReturn(List.of());
    subject =
        new DashboardApiKeyCheckTriggeredService(
            userAccountsApiMock, "admin", communityAuthorizationRepository, eventProducerMock);

    subject.accept(
        DashboardApiKeyCheckTriggered.builder()
            .communityAuthorizationId("b")
            .email("b@test.com")
            .dashboardApiKey("key")
            .index(1)
            .build());

    verifyNoInteractions(eventProducerMock);
  }

  @Test
  void accept_without_index_never_chains() {
    when(userAccountsApiMock.getUsersByCriteria(any(), any(), any(), any())).thenReturn(List.of());
    subject =
        new DashboardApiKeyCheckTriggeredService(
            userAccountsApiMock, "admin", communityAuthorizationRepository, eventProducerMock);

    subject.accept(
        DashboardApiKeyCheckTriggered.builder()
            .communityAuthorizationId("a")
            .email("a@test.com")
            .dashboardApiKey("key")
            .build());

    verifyNoInteractions(eventProducerMock);
    verify(communityAuthorizationRepository, never()).findAllIdsByIntegrationTestUsageFalse();
  }
}
