package app.bpartners.geojobs.service.event;

import static app.bpartners.geojobs.repository.model.detection.ZoneDetectionJob.DetectionType.MACHINE;
import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

import app.bpartners.geojobs.endpoint.event.model.zone.ZoneDetectionJobFailed;
import app.bpartners.geojobs.repository.DetectionRepository;
import app.bpartners.geojobs.repository.model.detection.Detection;
import app.bpartners.geojobs.repository.model.detection.ZoneDetectionJob;
import app.bpartners.geojobs.service.DetectionFinishedMailer;
import app.bpartners.geojobs.service.detection.ZoneDetectionJobService;
import app.bpartners.geojobs.template.HTMLTemplateParser;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class ZoneDetectionJobFailedServiceTest {
  DetectionFinishedMailer mailerMock = mock();
  ZoneDetectionJobService zoneDetectionJobServiceMock = mock();
  DetectionRepository detectionRepositoryMock = mock();
  HTMLTemplateParser htmlTemplateParser = new HTMLTemplateParser();
  ZoneDetectionJobFailedService subject =
      new ZoneDetectionJobFailedService(
          mailerMock, zoneDetectionJobServiceMock, detectionRepositoryMock, htmlTemplateParser);

  @Test
  void send_email_with_detection_e2Id() {
    var jobId = randomUUID().toString();
    var detectionE2Id = randomUUID().toString();
    var emailReceiver = "emailReceiver";
    var zoneDetectionJobMock = mock(ZoneDetectionJob.class);
    var detectionMock = mock(Detection.class);
    when(zoneDetectionJobMock.getId()).thenReturn(jobId);
    when(zoneDetectionJobMock.getEmailReceiver()).thenReturn(emailReceiver);
    when(detectionMock.getEndToEndId()).thenReturn(detectionE2Id);
    when(zoneDetectionJobServiceMock.findById(jobId)).thenReturn(zoneDetectionJobMock);
    when(detectionRepositoryMock.findByZdjId(jobId)).thenReturn(Optional.of(detectionMock));

    assertDoesNotThrow(() -> subject.accept(new ZoneDetectionJobFailed(jobId)));

    var stringCaptor = ArgumentCaptor.forClass(String.class);
    verify(mailerMock, only())
        .accept(
            eq(emailReceiver),
            eq("Erreur lors du traitement de la détection portant l'ID " + detectionE2Id),
            stringCaptor.capture());
    var emailBody = stringCaptor.getValue();
    assertEquals(getEmailBody(detectionMock), emailBody);
  }

  @Test
  void send_email_with_zdj_id() {
    var jobId = randomUUID().toString();
    var emailReceiver = "emailReceiver";
    var adminEmail = "tech@birdia.fr";
    var zoneDetectionJobMock = mock(ZoneDetectionJob.class);
    when(zoneDetectionJobMock.getId()).thenReturn(jobId);
    when(zoneDetectionJobMock.getEmailReceiver()).thenReturn(emailReceiver);
    when(zoneDetectionJobMock.getDetectionType()).thenReturn(MACHINE);
    when(zoneDetectionJobServiceMock.findById(jobId)).thenReturn(zoneDetectionJobMock);
    when(detectionRepositoryMock.findByZdjId(jobId)).thenReturn(Optional.empty());

    assertDoesNotThrow(() -> subject.accept(new ZoneDetectionJobFailed(jobId)));

    var stringCaptor = ArgumentCaptor.forClass(String.class);
    var emailSubject = "Erreur lors du traitement de la détection machine (ZDJ.id=" + jobId + ")";
    verify(mailerMock, only()).accept(eq(adminEmail), eq(emailSubject), stringCaptor.capture());
    var emailBody = stringCaptor.getValue();
    assertEquals(getEmailBody(zoneDetectionJobMock), emailBody);
  }

  private String getEmailBody(ZoneDetectionJob zoneDetectionJobMock) {
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
        <p>Une erreur est survenue lors du traitement du ZoneDetectionJob
            (type=<span>MACHINE</span>) portant l'ID <span>%s</span>.</p>
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
        zoneDetectionJobMock.getId());
  }

  private String getEmailBody(Detection detectionMock) {
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
        <p>Une erreur est survenue lors du traitement de la détection portant l'ID <span>%s</span> à l'étape 3 sur 4 : DETECTION MACHINE.</p>
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
        detectionMock.getEndToEndId());
  }
}
