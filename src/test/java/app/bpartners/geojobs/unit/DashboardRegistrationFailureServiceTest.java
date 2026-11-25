package app.bpartners.geojobs.unit;

import static app.bpartners.geojobs.repository.model.dashboard.DashboardRegistrationStatus.PENDING;
import static app.bpartners.geojobs.repository.model.dashboard.DashboardRegistrationStatus.REPROCESSED;
import static app.bpartners.geojobs.repository.model.dashboard.DashboardRegistrationType.DETECTION_2D;
import static app.bpartners.geojobs.repository.model.dashboard.DashboardRegistrationType.THREE_D_3D;
import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import app.bpartners.geojobs.endpoint.event.EventProducer;
import app.bpartners.geojobs.endpoint.event.model.DetectionTrackingRegistrationRequested;
import app.bpartners.geojobs.endpoint.event.model.ThreeDTrackingRegistrationRequested;
import app.bpartners.geojobs.mail.Email;
import app.bpartners.geojobs.mail.Mailer;
import app.bpartners.geojobs.model.exception.BadRequestException;
import app.bpartners.geojobs.model.exception.NotFoundException;
import app.bpartners.geojobs.repository.DashboardRegistrationFailureRepository;
import app.bpartners.geojobs.repository.model.dashboard.DashboardRegistrationFailure;
import app.bpartners.geojobs.service.DashboardRegistrationFailureService;
import app.bpartners.geojobs.service.dashboard.component.Register3d;
import app.bpartners.geojobs.template.HTMLTemplateParser;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.web.client.ResourceAccessException;

class DashboardRegistrationFailureServiceTest {
  DashboardRegistrationFailureRepository repositoryMock = mock();
  EventProducer eventProducerMock = mock();
  Mailer mailerMock = mock();
  HTMLTemplateParser htmlTemplateParserMock = mock();
  DashboardRegistrationFailureService subject =
      new DashboardRegistrationFailureService(
          repositoryMock, eventProducerMock, mailerMock, htmlTemplateParserMock, ADMIN_EMAIL);

  static final String ADMIN_EMAIL = "admin@email.com";
  final String subjectId = randomUUID().toString();
  final String communityOwnerId = randomUUID().toString();

  @Test
  void record_persists_a_pending_failure_with_the_register_to_replay() {
    when(repositoryMock.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    when(htmlTemplateParserMock.apply(any(), any())).thenReturn("<html></html>");

    var actual =
        subject.recordFailure(
            THREE_D_3D,
            subjectId,
            communityOwnerId,
            "city-json:" + subjectId,
            new Register3d("1 rue de la Paix", "crm-42"),
            new ResourceAccessException("connection refused"));

    assertNotNull(actual.getId());
    assertEquals(THREE_D_3D, actual.getType());
    assertEquals(subjectId, actual.getSubjectId());
    assertEquals(communityOwnerId, actual.getCommunityOwnerId());
    assertEquals("city-json:" + subjectId, actual.getProjectKey());
    assertEquals(
        "{\"address\":\"1 rue de la Paix\",\"externalReference\":\"crm-42\"}", actual.getPayload());
    assertTrue(actual.getErrorMessage().contains("connection refused"));
    assertEquals(1, actual.getAttempts());
    assertEquals(PENDING, actual.getStatus());
  }

  @Test
  void record_notifies_the_admin_by_email() {
    when(repositoryMock.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    when(htmlTemplateParserMock.apply(any(), any())).thenReturn("<html>body</html>");

    subject.recordFailure(
        DETECTION_2D,
        subjectId,
        communityOwnerId,
        "key",
        new Register3d(null),
        new RuntimeException());

    var emailCaptor = ArgumentCaptor.forClass(Email.class);
    verify(mailerMock).accept(emailCaptor.capture());
    var email = emailCaptor.getValue();
    assertEquals(ADMIN_EMAIL, email.to().getAddress());
    assertEquals("<html>body</html>", email.htmlBody());
    assertTrue(email.subject().contains("DETECTION_2D"));
  }

  @Test
  void record_updates_the_pending_failure_of_the_same_subject() {
    var pending =
        DashboardRegistrationFailure.builder()
            .id("existing")
            .type(DETECTION_2D)
            .subjectId(subjectId)
            .attempts(1)
            .status(PENDING)
            .build();
    when(repositoryMock.findFirstByTypeAndSubjectIdAndStatus(DETECTION_2D, subjectId, PENDING))
        .thenReturn(Optional.of(pending));
    when(repositoryMock.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    when(htmlTemplateParserMock.apply(any(), any())).thenReturn("<html></html>");

    var actual =
        subject.recordFailure(
            DETECTION_2D,
            subjectId,
            communityOwnerId,
            "key",
            new Register3d(null),
            new RuntimeException("still down"));

    assertEquals("existing", actual.getId());
    assertEquals(2, actual.getAttempts());
    assertEquals("still down", actual.getErrorMessage());
  }

  @Test
  void record_survives_a_failing_notification() {
    when(repositoryMock.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    when(htmlTemplateParserMock.apply(any(), any())).thenReturn("<html></html>");
    doThrow(new RuntimeException("ses down")).when(mailerMock).accept(any());

    var actual =
        subject.recordFailure(
            THREE_D_3D,
            subjectId,
            communityOwnerId,
            "key",
            new Register3d(null),
            new RuntimeException());

    assertEquals(PENDING, actual.getStatus());
    verify(repositoryMock).save(any());
  }

  @Test
  void resolve_closes_the_pending_failure() {
    var pending =
        DashboardRegistrationFailure.builder()
            .id("existing")
            .type(THREE_D_3D)
            .subjectId(subjectId)
            .status(PENDING)
            .build();
    when(repositoryMock.findById("existing")).thenReturn(Optional.of(pending));

    subject.resolve("existing");

    var captor = ArgumentCaptor.forClass(DashboardRegistrationFailure.class);
    verify(repositoryMock).save(captor.capture());
    assertEquals(REPROCESSED, captor.getValue().getStatus());
    assertNotNull(captor.getValue().getReprocessedDatetime());
  }

  @Test
  void resolve_does_nothing_for_an_unknown_failure() {
    when(repositoryMock.findById("unknown")).thenReturn(Optional.empty());

    subject.resolve("unknown");

    verify(repositoryMock, never()).save(any());
  }

  @Test
  void resolve_does_nothing_for_an_already_reprocessed_failure() {
    when(repositoryMock.findById("done"))
        .thenReturn(
            Optional.of(
                DashboardRegistrationFailure.builder()
                    .id("done")
                    .type(THREE_D_3D)
                    .status(REPROCESSED)
                    .build()));

    subject.resolve("done");

    verify(repositoryMock, never()).save(any());
  }

  @Test
  void reprocess_emits_the_detection_registration_event_again() {
    when(repositoryMock.findById("id"))
        .thenReturn(
            Optional.of(
                DashboardRegistrationFailure.builder()
                    .id("id")
                    .type(DETECTION_2D)
                    .subjectId(subjectId)
                    .status(PENDING)
                    .build()));

    subject.reprocess("id");

    verify(eventProducerMock)
        .accept(List.of(new DetectionTrackingRegistrationRequested(subjectId, "id")));
  }

  @Test
  void reprocess_emits_the_3d_registration_event_again() {
    when(repositoryMock.findById("id"))
        .thenReturn(
            Optional.of(
                DashboardRegistrationFailure.builder()
                    .id("id")
                    .type(THREE_D_3D)
                    .subjectId(subjectId)
                    .communityOwnerId(communityOwnerId)
                    .status(PENDING)
                    .build()));

    subject.reprocess("id");

    verify(eventProducerMock)
        .accept(
            List.of(new ThreeDTrackingRegistrationRequested(subjectId, communityOwnerId, "id")));
  }

  @Test
  void reprocess_refuses_an_already_reprocessed_failure() {
    when(repositoryMock.findById("id"))
        .thenReturn(
            Optional.of(
                DashboardRegistrationFailure.builder()
                    .id("id")
                    .type(THREE_D_3D)
                    .status(REPROCESSED)
                    .build()));

    assertThrows(BadRequestException.class, () -> subject.reprocess("id"));
    verify(eventProducerMock, never()).accept(any());
  }

  @Test
  void reprocess_unknown_failure_is_not_found() {
    when(repositoryMock.findById("unknown")).thenReturn(Optional.empty());

    assertThrows(NotFoundException.class, () -> subject.reprocess("unknown"));
  }
}
