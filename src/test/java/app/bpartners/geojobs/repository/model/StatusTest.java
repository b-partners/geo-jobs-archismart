package app.bpartners.geojobs.repository.model;

import static app.bpartners.geojobs.job.model.Status.HealthStatus.FAILED;
import static app.bpartners.geojobs.job.model.Status.HealthStatus.SUCCEEDED;
import static app.bpartners.geojobs.job.model.Status.HealthStatus.UNKNOWN;
import static app.bpartners.geojobs.job.model.Status.ProgressionStatus.FINISHED;
import static app.bpartners.geojobs.job.model.Status.ProgressionStatus.PENDING;
import static app.bpartners.geojobs.job.model.Status.ProgressionStatus.PROCESSING;
import static java.lang.Thread.sleep;
import static java.time.Instant.now;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import app.bpartners.geojobs.job.model.Status;
import app.bpartners.geojobs.job.model.Status.HealthStatus;
import app.bpartners.geojobs.job.model.Status.ProgressionStatus;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class StatusTest {

  @Test
  void if_same_progression_and_health_then_take_most_recent() throws InterruptedException {
    var oldStatus = aStatus(PROCESSING, UNKNOWN, now());
    sleep(1_000);
    var newStatus = aStatus(PROCESSING, UNKNOWN, now());
    assertEquals(newStatus, oldStatus.to(newStatus));
  }

  @Test
  void ignore_old_status_if_a_new_one_is_known() throws InterruptedException {
    var oldStatus = aStatus(PROCESSING, UNKNOWN, now());
    sleep(1_000);
    var newStatus = aStatus(FINISHED, SUCCEEDED, now());
    assertEquals(newStatus, newStatus.to(oldStatus));
  }

  @Test
  void finished_is_terminal() {
    assertThrows(
        IllegalArgumentException.class,
        () -> aStatus(FINISHED, FAILED).to(aStatus(PROCESSING, UNKNOWN)));
    assertThrows(
        IllegalArgumentException.class,
        () -> aStatus(FINISHED, FAILED).to(aStatus(PENDING, UNKNOWN)));
  }

  private static Status aStatus(
      ProgressionStatus progression, HealthStatus health, Instant instant) {
    var status = new Status();
    status.setProgression(progression);
    status.setHealth(health);
    status.setCreationDatetime(instant);
    return status;
  }

  private static Status aStatus(ProgressionStatus progression, HealthStatus health) {
    return aStatus(progression, health, now());
  }
}
