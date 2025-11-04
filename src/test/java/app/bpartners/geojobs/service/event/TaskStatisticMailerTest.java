package app.bpartners.geojobs.service.event;

import static app.bpartners.geojobs.repository.model.GeoJobType.DETECTION;
import static app.bpartners.geojobs.repository.model.GeoJobType.TILING;
import static app.bpartners.geojobs.service.TaskStatisticMailer.TASK_STATISTIC_MAILER_TEMPLATE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

import app.bpartners.geojobs.job.model.statistic.TaskStatistic;
import app.bpartners.geojobs.mail.Email;
import app.bpartners.geojobs.mail.Mailer;
import app.bpartners.geojobs.repository.model.detection.ZoneDetectionJob;
import app.bpartners.geojobs.repository.model.tiling.ZoneTilingJob;
import app.bpartners.geojobs.service.TaskStatisticMailer;
import app.bpartners.geojobs.template.HTMLTemplateParser;
import jakarta.mail.internet.InternetAddress;
import java.time.Instant;
import java.util.List;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.thymeleaf.context.Context;

public class TaskStatisticMailerTest {
  Mailer mailerMock = mock();
  HTMLTemplateParser htmlTemplateParserMock = mock();
  TaskStatisticMailer subject = new TaskStatisticMailer(mailerMock, htmlTemplateParserMock);

  @SneakyThrows
  @Test
  void accept_detection_job_ok() {
    String emailBody = "emailBody";
    Instant now = Instant.now();
    String emailReceiver = "dummyEmail";
    String jobId = "jobId";
    String zoneName = "dummyZoneName";
    String env = System.getenv("ENV");
    TaskStatistic taskStatistic = TaskStatistic.builder().updatedAt(now).build();
    ZoneDetectionJob detectionJob =
        ZoneDetectionJob.builder()
            .id(jobId)
            .zoneName(zoneName)
            .emailReceiver(emailReceiver)
            .build();
    String emailSubject =
        "[geo-jobs/"
            + env
            + "] Statistiques du job (id="
            + jobId
            + ", zone="
            + zoneName
            + ", type="
            + detectionJob.getStatus().getJobType()
            + ") du "
            + now;
    when(htmlTemplateParserMock.apply(any(), any())).thenReturn(emailBody);

    subject.accept(taskStatistic, detectionJob);

    var contextCaptor = ArgumentCaptor.forClass(Context.class);
    var emailCaptor = ArgumentCaptor.forClass(Email.class);
    verify(htmlTemplateParserMock, times(1))
        .apply(eq(TASK_STATISTIC_MAILER_TEMPLATE), contextCaptor.capture());
    verify(mailerMock, times(1)).accept(emailCaptor.capture());
    var contextCaptured = contextCaptor.getValue();
    var emailCaptured = emailCaptor.getValue();
    Email expectedEmail =
        new Email(
            new InternetAddress(emailReceiver),
            List.of(),
            List.of(),
            emailSubject,
            emailBody,
            List.of());
    assertEquals(expectedEmail, emailCaptured);
    assertEquals(taskStatistic, contextCaptured.getVariable("taskStatistic"));
    assertEquals(detectionJob, contextCaptured.getVariable("job"));
    assertEquals(DETECTION, contextCaptured.getVariable("jobType"));
  }

  @SneakyThrows
  @Test
  void accept_tiling_job_ok() {
    String emailBody = "emailBody";
    Instant now = Instant.now();
    String emailReceiver = "dummyEmail";
    String jobId = "jobId";
    String zoneName = "dummyZoneName";
    String env = System.getenv("ENV");
    TaskStatistic taskStatistic = TaskStatistic.builder().updatedAt(now).build();
    ZoneTilingJob detectionJob =
        ZoneTilingJob.builder().id(jobId).zoneName(zoneName).emailReceiver(emailReceiver).build();
    String emailSubject =
        "[geo-jobs/"
            + env
            + "] Statistiques du job (id="
            + jobId
            + ", zone="
            + zoneName
            + ", type="
            + detectionJob.getStatus().getJobType()
            + ") du "
            + now;
    when(htmlTemplateParserMock.apply(any(), any())).thenReturn(emailBody);

    subject.accept(taskStatistic, detectionJob);

    var contextCaptor = ArgumentCaptor.forClass(Context.class);
    var emailCaptor = ArgumentCaptor.forClass(Email.class);
    verify(htmlTemplateParserMock, times(1))
        .apply(eq(TASK_STATISTIC_MAILER_TEMPLATE), contextCaptor.capture());
    verify(mailerMock, times(1)).accept(emailCaptor.capture());
    var contextCaptured = contextCaptor.getValue();
    var emailCaptured = emailCaptor.getValue();
    Email expectedEmail =
        new Email(
            new InternetAddress(emailReceiver),
            List.of(),
            List.of(),
            emailSubject,
            emailBody,
            List.of());
    assertEquals(expectedEmail, emailCaptured);
    assertEquals(taskStatistic, contextCaptured.getVariable("taskStatistic"));
    assertEquals(detectionJob, contextCaptured.getVariable("job"));
    assertEquals(TILING, contextCaptured.getVariable("jobType"));
  }
}
