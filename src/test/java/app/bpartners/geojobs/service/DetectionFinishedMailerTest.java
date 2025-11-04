package app.bpartners.geojobs.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

import app.bpartners.geojobs.mail.Email;
import app.bpartners.geojobs.mail.Mailer;
import jakarta.mail.internet.InternetAddress;
import java.util.List;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class DetectionFinishedMailerTest {
  Mailer mailerMock = mock(Mailer.class);
  DetectionFinishedMailer subject = new DetectionFinishedMailer(mailerMock);

  @SneakyThrows
  @Test
  void send_email() {
    String emailReceiver = "emailReceiver";
    String emailSubject = "Analyse sur la zone zoneName terminée le 01/01/2025 08:00:00";
    var emailBody = "Email body";

    assertDoesNotThrow(() -> subject.accept(emailReceiver, emailSubject, emailBody));

    var emailCaptor = ArgumentCaptor.forClass(Email.class);
    verify(mailerMock, only()).accept(emailCaptor.capture());
    var actualEmail = emailCaptor.getValue();
    assertEquals(
        new Email(
            new InternetAddress(emailReceiver),
            List.of(new InternetAddress("tech@birdia.fr")),
            List.of(),
            emailSubject,
            emailBody,
            List.of()),
        actualEmail);
  }
}
