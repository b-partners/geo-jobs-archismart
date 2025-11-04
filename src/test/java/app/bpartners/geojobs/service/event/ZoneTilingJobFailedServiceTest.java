package app.bpartners.geojobs.service.event;

import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

import app.bpartners.geojobs.endpoint.event.model.zone.ZoneTilingJobFailed;
import app.bpartners.geojobs.repository.DetectionRepository;
import app.bpartners.geojobs.repository.model.detection.Detection;
import app.bpartners.geojobs.repository.model.tiling.ZoneTilingJob;
import app.bpartners.geojobs.service.DetectionFinishedMailer;
import app.bpartners.geojobs.template.HTMLTemplateParser;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class ZoneTilingJobFailedServiceTest {
  DetectionFinishedMailer detectionFinishedMailerMock = mock();
  DetectionRepository detectionRepositoryMock = mock();
  HTMLTemplateParser htmlTemplateParser = new HTMLTemplateParser();
  ZoneTilingJobFailedService subject =
      new ZoneTilingJobFailedService(
          detectionFinishedMailerMock, detectionRepositoryMock, htmlTemplateParser);

  @Test
  void trigger_email_containing_detection_id() {
    var jobId = randomUUID().toString();
    var detectionE2Id = randomUUID().toString();
    var emailReceiver = "dummy@email.com";
    var zoneTilingJob = mock(ZoneTilingJob.class);
    var detectionMock = mock(Detection.class);
    when(zoneTilingJob.getId()).thenReturn(jobId);
    when(zoneTilingJob.getEmailReceiver()).thenReturn(emailReceiver);
    when(detectionMock.getEndToEndId()).thenReturn(detectionE2Id);
    when(detectionRepositoryMock.findByZtjId(jobId)).thenReturn(Optional.of(detectionMock));

    assertDoesNotThrow(() -> subject.accept(new ZoneTilingJobFailed(zoneTilingJob)));

    var stringCaptor = ArgumentCaptor.forClass(String.class);
    verify(detectionFinishedMailerMock, only())
        .accept(
            eq(emailReceiver),
            eq("Erreur survenue lors du traitement de la détection portant l'ID " + detectionE2Id),
            stringCaptor.capture());
    assertEquals(getEmailBody(detectionMock), stringCaptor.getValue());
  }

  @Test
  void trigger_email_containing_tiling_job_id() {
    var jobId = randomUUID().toString();
    var emailReceiver = "dummy@email.com";
    var zoneTilingJob = mock(ZoneTilingJob.class);
    when(zoneTilingJob.getId()).thenReturn(jobId);
    when(zoneTilingJob.getEmailReceiver()).thenReturn(emailReceiver);
    when(detectionRepositoryMock.findByZtjId(jobId)).thenReturn(Optional.empty());

    assertDoesNotThrow(() -> subject.accept(new ZoneTilingJobFailed(zoneTilingJob)));

    var stringCaptor = ArgumentCaptor.forClass(String.class);
    verify(detectionFinishedMailerMock, only())
        .accept(
            eq(emailReceiver),
            eq("Erreur survenue lors du traitement du pavage (ZTJ.id=" + jobId + ")"),
            stringCaptor.capture());
    assertEquals(getEmailBody(zoneTilingJob), stringCaptor.getValue());
  }

  private String getEmailBody(Detection detection) {
    return String.format(
        """
<html>
<head>
    <style>
        body {
            font-family: Helvetica, serif;
        }
    </style>
</head>
<body>
<section>
    <p>Bonjour,</p>
    <div>
        <p>Une erreur est survenue lors du traitement de la détection portant l'ID <span>%s</span> à l'étape 2 sur 4 : RÉCUPÉRATION DES IMAGES.</p>
    </div>
   \s
    <p>Veuillez réessayer plus tard ou nous contacter par email à l'adresse
        <a href="mailto:contact@birdia.fr">contact@birdia.fr</a> ou au numéro 01 84 80 31 69 pour toutes
        informations supplémentaires.
    </p>
    <p>Cordialement,</p>
    <p>L'équipe BirdIA.</p>
</section>
</body>
</html>""",
        detection.getEndToEndId());
  }

  private String getEmailBody(ZoneTilingJob zoneTilingJob) {
    return String.format(
        """
<html>
<head>
    <style>
        body {
            font-family: Helvetica, serif;
        }
    </style>
</head>
<body>
<section>
    <p>Bonjour,</p>
   \s
    <div>
        <p>Une erreur est survenue lors du traitement du ZoneTilingJob portant l'ID <span>%s</span>.</p>
    </div>
    <p>Veuillez réessayer plus tard ou nous contacter par email à l'adresse
        <a href="mailto:contact@birdia.fr">contact@birdia.fr</a> ou au numéro 01 84 80 31 69 pour toutes
        informations supplémentaires.
    </p>
    <p>Cordialement,</p>
    <p>L'équipe BirdIA.</p>
</section>
</body>
</html>""",
        zoneTilingJob.getId());
  }
}
