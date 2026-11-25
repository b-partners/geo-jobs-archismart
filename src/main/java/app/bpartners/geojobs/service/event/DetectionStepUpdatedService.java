package app.bpartners.geojobs.service.event;

import static java.time.Instant.now;

import app.bpartners.geojobs.endpoint.event.model.DetectionStepUpdated;
import app.bpartners.geojobs.mail.Email;
import app.bpartners.geojobs.mail.Mailer;
import app.bpartners.geojobs.repository.model.detection.Detection;
import app.bpartners.geojobs.service.EmailRecipientResolver;
import app.bpartners.geojobs.template.HTMLTemplateParser;
import jakarta.mail.internet.AddressException;
import jakarta.mail.internet.InternetAddress;
import java.io.File;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.function.Consumer;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;

@Service
@AllArgsConstructor
public class DetectionStepUpdatedService implements Consumer<DetectionStepUpdated> {
  public final Mailer mailer;
  private final EmailRecipientResolver emailRecipientResolver;
  private static final String DETECTION_STEP_UPDATED = "detection_step_updated";

  @Override
  public void accept(DetectionStepUpdated detectionStepUpdated) {
    var detection = detectionStepUpdated.getDetection();
    List<InternetAddress> bcc = List.of();
    var env = System.getenv("ENV");
    String subject =
        String.format(
            "[%s] Mise à jour de la detection(e2Id=%s, communityOwnerId=%s) le %s",
            env == null ? "" : env.toLowerCase(),
            detection.getEndToEndId(),
            detection.getCommunityOwnerId(),
            DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm")
                .format(now().atZone(ZoneId.of("Europe/Paris"))));
    String htmlBody = computeStaticDetectionStepUpdateEmailBody(detection);
    List<File> attachments = List.of();
    var to = emailRecipientResolver.resolve(detection, detection.getEmailReceiver());
    try {
      mailer.accept(
          new Email(
              new InternetAddress(to),
              emailRecipientResolver.adminCopyUnless(to),
              bcc,
              subject,
              htmlBody,
              attachments));
    } catch (AddressException e) {
      throw new RuntimeException(e);
    }
  }

  public static String computeStaticDetectionStepUpdateEmailBody(Detection detection) {
    var htmlTemplateParser = new HTMLTemplateParser();
    Context context = new Context();
    context.setVariable("detection", detection);
    context.setVariable("phase", detection.getStep().getName());
    context.setVariable("health", detection.getStep().getHealth());
    return htmlTemplateParser.apply(DETECTION_STEP_UPDATED, context);
  }
}
