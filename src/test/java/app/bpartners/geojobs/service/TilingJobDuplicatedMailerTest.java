package app.bpartners.geojobs.service;

import static app.bpartners.geojobs.job.model.Status.HealthStatus.SUCCEEDED;
import static app.bpartners.geojobs.job.model.Status.HealthStatus.UNKNOWN;
import static app.bpartners.geojobs.job.model.Status.ProgressionStatus.FINISHED;
import static app.bpartners.geojobs.job.model.Status.ProgressionStatus.PENDING;
import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

import app.bpartners.geojobs.job.model.JobStatus;
import app.bpartners.geojobs.mail.Email;
import app.bpartners.geojobs.mail.Mailer;
import app.bpartners.geojobs.repository.model.DuplicatedTilingJob;
import app.bpartners.geojobs.service.tiling.TilingJobDuplicatedMailer;
import app.bpartners.geojobs.template.HTMLTemplateParser;
import jakarta.mail.internet.InternetAddress;
import java.util.List;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class TilingJobDuplicatedMailerTest {
  HTMLTemplateParser htmlTemplateParserMock = mock();
  Mailer mailerMock = mock();
  TilingJobDuplicatedMailer subject =
      new TilingJobDuplicatedMailer(mailerMock, htmlTemplateParserMock);

  @SneakyThrows
  @Test
  void accept_ok() {
    String emailBody = "emailBody";
    when(htmlTemplateParserMock.apply(any(), any())).thenReturn(emailBody);
    var originalJob = aZTJ("originalJobId", FINISHED, SUCCEEDED);
    var duplicatedJob = aZTJ("duplicatedJobId", PENDING, UNKNOWN);

    assertDoesNotThrow(() -> subject.accept(new DuplicatedTilingJob(originalJob, duplicatedJob)));

    var emailCaptor = ArgumentCaptor.forClass(Email.class);
    verify(mailerMock, times(1)).accept(emailCaptor.capture());
    var emailCaptured = emailCaptor.getValue();
    assertEquals(
        new Email(
            new InternetAddress(originalJob.getEmailReceiver()),
            List.of(),
            List.of(),
            "[geo-jobs/null] Duplication du job de pavage (id=originalJobId) términée",
            emailBody,
            List.of()),
        emailCaptured);
  }

  private static app.bpartners.geojobs.repository.model.tiling.ZoneTilingJob aZTJ(
      String jobId,
      app.bpartners.geojobs.job.model.Status.ProgressionStatus progressionStatus,
      app.bpartners.geojobs.job.model.Status.HealthStatus healthStatus) {
    return app.bpartners.geojobs.repository.model.tiling.ZoneTilingJob.builder()
        .id(jobId)
        .zoneName("dummy")
        .emailReceiver("dummy")
        .statusHistory(
            List.of(
                JobStatus.builder()
                    .id(randomUUID().toString())
                    .jobId(jobId)
                    .progression(progressionStatus)
                    .health(healthStatus)
                    .build()))
        .build();
  }
}
