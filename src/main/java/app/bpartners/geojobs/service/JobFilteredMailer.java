package app.bpartners.geojobs.service;

import app.bpartners.geojobs.job.model.Job;
import app.bpartners.geojobs.mail.Email;
import app.bpartners.geojobs.mail.Mailer;
import app.bpartners.geojobs.repository.model.FilteredJob;
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
public class JobFilteredMailer<J extends Job> implements Consumer<FilteredJob<J>> {
  private final Mailer mailer;
  public static final String TEMPLATE_NAME = "job_filtering";
  private final HTMLTemplateParser htmlTemplateParser;
  private final String env = System.getenv("ENV");

  @SneakyThrows
  @Override
  public void accept(FilteredJob<J> filteredJob) {
    var initialJobId = filteredJob.getInitialJobId();
    var succeededJob = filteredJob.getSucceededJob();
    var notSucceededJob = filteredJob.getNotSucceededJob();
    Context context = new Context();
    context.setVariable("initialJobId", initialJobId);
    context.setVariable("succeededJob", succeededJob);
    context.setVariable("notSucceededJob", notSucceededJob);
    String emailBody = htmlTemplateParser.apply(TEMPLATE_NAME, context);

    mailer.accept(
        new Email(
            new InternetAddress(succeededJob.getEmailReceiver()),
            List.of(),
            List.of(),
            "[geo-jobs/"
                + env
                + "] Séparation des tâches du job (id="
                + initialJobId
                + ","
                + " type="
                + succeededJob.getStatus().getJobType()
                + ") términée",
            emailBody,
            List.of()));
  }
}
