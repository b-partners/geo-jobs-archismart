package app.bpartners.geojobs.repository;

import static app.bpartners.geojobs.repository.model.dashboard.DashboardRegistrationStatus.PENDING;
import static app.bpartners.geojobs.repository.model.dashboard.DashboardRegistrationStatus.REPROCESSED;
import static app.bpartners.geojobs.repository.model.dashboard.DashboardRegistrationType.DETECTION_2D;
import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import app.bpartners.geojobs.conf.FacadeIT;
import app.bpartners.geojobs.repository.model.dashboard.DashboardRegistrationFailure;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class DashboardRegistrationFailureRepositoryIT extends FacadeIT {
  @Autowired DashboardRegistrationFailureRepository subject;

  @Test
  void save_and_find_the_pending_failure_of_a_subject() {
    var subjectId = randomUUID().toString();
    var now = Instant.now().truncatedTo(ChronoUnit.MICROS);
    var saved =
        subject.save(
            DashboardRegistrationFailure.builder()
                .id(randomUUID().toString())
                .type(DETECTION_2D)
                .subjectId(subjectId)
                .communityOwnerId("community")
                .projectKey("v1:key")
                .payload("{\"zone\":\"zone\"}")
                .errorMessage("connection refused")
                .attempts(1)
                .status(PENDING)
                .creationDatetime(now)
                .lastAttemptDatetime(now)
                .build());

    var actual = subject.findFirstByTypeAndSubjectIdAndStatus(DETECTION_2D, subjectId, PENDING);

    assertTrue(actual.isPresent());
    assertEquals(saved.getId(), actual.get().getId());
    assertEquals("{\"zone\":\"zone\"}", actual.get().getPayload());
    assertEquals(now, actual.get().getCreationDatetime());
  }

  @Test
  void reprocessed_failure_is_no_longer_pending() {
    var subjectId = randomUUID().toString();
    var saved =
        subject.save(
            DashboardRegistrationFailure.builder()
                .id(randomUUID().toString())
                .type(DETECTION_2D)
                .subjectId(subjectId)
                .attempts(1)
                .status(PENDING)
                .build());

    subject.save(saved.toBuilder().status(REPROCESSED).reprocessedDatetime(Instant.now()).build());

    assertTrue(
        subject.findFirstByTypeAndSubjectIdAndStatus(DETECTION_2D, subjectId, PENDING).isEmpty());
    assertTrue(
        subject
            .findFirstByTypeAndSubjectIdAndStatus(DETECTION_2D, subjectId, REPROCESSED)
            .isPresent());
  }
}
