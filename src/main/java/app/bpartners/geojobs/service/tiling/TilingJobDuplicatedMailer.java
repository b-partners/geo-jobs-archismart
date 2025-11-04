package app.bpartners.geojobs.service.tiling;

import app.bpartners.geojobs.mail.Email;
import app.bpartners.geojobs.mail.Mailer;
import app.bpartners.geojobs.repository.model.DuplicatedTilingJob;
import app.bpartners.geojobs.template.HTMLTemplateParser;
import jakarta.mail.internet.InternetAddress;
import java.util.List;
import java.util.function.Consumer;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;

@Service
@AllArgsConstructor
@Slf4j
public class TilingJobDuplicatedMailer implements Consumer<DuplicatedTilingJob> {
  private final Mailer mailer;
  private static final String TEMPLATE_NAME = "tiling_duplicated";
  private final HTMLTemplateParser htmlTemplateParser;
  private final String env = System.getenv("ENV");

  @SneakyThrows
  @Override
  public void accept(DuplicatedTilingJob duplicatedTilingJob) {
    var originalJob = duplicatedTilingJob.getOriginalJob();
    var duplicatedJob = duplicatedTilingJob.getDuplicatedJob();
    Context context = new Context();
    context.setVariable("originalJob", originalJob);
    context.setVariable("duplicatedJob", duplicatedJob);
    String emailBody = htmlTemplateParser.apply(TEMPLATE_NAME, context);

    mailer.accept(
        new Email(
            new InternetAddress(duplicatedJob.getEmailReceiver()),
            List.of(),
            List.of(),
            "[geo-jobs/"
                + env
                + "] Duplication du job de pavage (id="
                + originalJob.getId()
                + ") términée",
            emailBody,
            List.of()));
    log.info("TilingJob duplication finished. Email sent to {}", duplicatedJob.getEmailReceiver());
  }
}
