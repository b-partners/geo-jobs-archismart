package app.bpartners.geojobs.service;

import app.bpartners.geojobs.job.model.Job;
import app.bpartners.geojobs.job.model.JobStatus;
import app.bpartners.geojobs.mail.Email;
import app.bpartners.geojobs.mail.Mailer;
import app.bpartners.geojobs.template.HTMLTemplateParser;
import jakarta.mail.internet.InternetAddress;
import java.util.List;
import java.util.function.Consumer;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;

@Service
@AllArgsConstructor
public class JobFinishedMailer<J extends Job> implements Consumer<J> {
  public static final String JOB_FINISHED_TEMPLATE = "job_finished";
  private final Mailer mailer;
  private final HTMLTemplateParser htmlTemplateParser;
  private final String env = System.getenv("ENV");

  @SneakyThrows
  @Override
  public void accept(J job) {
    JobStatus jobStatus = job.getStatus();
    Context context = new Context();
    String zoneName = job.getZoneName();
    context.setVariable("job", job);
    context.setVariable("status", String.valueOf(jobStatus));
    String emailBody = htmlTemplateParser.apply(JOB_FINISHED_TEMPLATE, context);

    mailer.accept(
        new Email(
            new InternetAddress("tech@birdia.fr"),
            List.of(),
            List.of(),
            "[geo-jobs/"
                + env
                + "] Fin du job (id="
                + job.getId()
                + ", type="
                + jobStatus.getJobType()
                + ", zone="
                + zoneName
                + ")",
            emailBody,
            List.of()));
  }
}
