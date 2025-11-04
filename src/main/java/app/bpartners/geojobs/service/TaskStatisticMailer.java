package app.bpartners.geojobs.service;

import app.bpartners.geojobs.job.model.Job;
import app.bpartners.geojobs.job.model.JobType;
import app.bpartners.geojobs.job.model.statistic.TaskStatistic;
import app.bpartners.geojobs.mail.Email;
import app.bpartners.geojobs.mail.Mailer;
import app.bpartners.geojobs.template.HTMLTemplateParser;
import jakarta.mail.internet.InternetAddress;
import java.util.List;
import java.util.function.BiConsumer;
import lombok.SneakyThrows;
import org.thymeleaf.context.Context;

public class TaskStatisticMailer<J extends Job> implements BiConsumer<TaskStatistic, J> {
  public static final String TASK_STATISTIC_MAILER_TEMPLATE = "task_statistic";
  private final Mailer mailer;
  private final HTMLTemplateParser htmlTemplateParser;
  private final String env = System.getenv("ENV");

  public TaskStatisticMailer(Mailer mailer, HTMLTemplateParser htmlTemplateParser) {
    this.mailer = mailer;
    this.htmlTemplateParser = htmlTemplateParser;
  }

  @SneakyThrows
  @Override
  public void accept(TaskStatistic taskStatistic, J job) {
    JobType jobType = job.getStatus().getJobType();
    Context context = new Context();
    context.setVariable("taskStatistic", taskStatistic);
    context.setVariable("job", job);
    context.setVariable("jobType", jobType);
    String emailBody = htmlTemplateParser.apply(TASK_STATISTIC_MAILER_TEMPLATE, context);
    String subject =
        "[geo-jobs/"
            + env
            + "] Statistiques du job (id="
            + job.getId()
            + ", zone="
            + job.getZoneName()
            + ", type="
            + jobType
            + ") du "
            + taskStatistic.getUpdatedAt();
    mailer.accept(
        new Email(
            new InternetAddress(job.getEmailReceiver()),
            List.of(),
            List.of(),
            subject,
            emailBody,
            List.of()));
  }
}
