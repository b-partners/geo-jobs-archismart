package app.bpartners.geojobs.service;

import static app.bpartners.geojobs.repository.model.GeoJobType.DETECTION;
import static app.bpartners.geojobs.service.JobFilteredMailer.TEMPLATE_NAME;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

import app.bpartners.geojobs.job.model.JobStatus;
import app.bpartners.geojobs.mail.Mailer;
import app.bpartners.geojobs.repository.model.FilteredJob;
import app.bpartners.geojobs.repository.model.detection.ZoneDetectionJob;
import app.bpartners.geojobs.template.HTMLTemplateParser;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.thymeleaf.context.Context;

public class JobFilteredMailerTest {
  private static final String INITIAL_JOB_ID = "initialJobId";
  Mailer mailerMock = mock();
  HTMLTemplateParser htmlTemplateParserMock = mock();
  JobFilteredMailer<ZoneDetectionJob> detectionSubject =
      new JobFilteredMailer<>(mailerMock, htmlTemplateParserMock);

  @Test
  void accept_ok() {
    ZoneDetectionJob succeededJob =
        ZoneDetectionJob.builder()
            .id("succeededJobId")
            .emailReceiver("dummy@email.com")
            .statusHistory(List.of(JobStatus.builder().jobType(DETECTION).build()))
            .build();
    ZoneDetectionJob notSucceededJob =
        ZoneDetectionJob.builder()
            .id("notSucceededJobId")
            .emailReceiver("dummy@email.com")
            .statusHistory(List.of(JobStatus.builder().jobType(DETECTION).build()))
            .build();
    assertDoesNotThrow(
        () ->
            detectionSubject.accept(
                new FilteredJob<>(INITIAL_JOB_ID, succeededJob, notSucceededJob)));

    var contextArgCaptor = ArgumentCaptor.forClass(Context.class);
    verify(htmlTemplateParserMock, times(1)).apply(eq(TEMPLATE_NAME), contextArgCaptor.capture());
    var contextValue = contextArgCaptor.getValue();
    assertEquals(INITIAL_JOB_ID, contextValue.getVariable("initialJobId"));
    assertEquals(succeededJob, contextValue.getVariable("succeededJob"));
    assertEquals(notSucceededJob, contextValue.getVariable("notSucceededJob"));
  }
}
