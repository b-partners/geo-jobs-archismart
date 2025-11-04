package app.bpartners.geojobs.service.event;

import app.bpartners.geojobs.endpoint.event.model.zone.ZoneTilingJobFailed;
import app.bpartners.geojobs.repository.DetectionRepository;
import app.bpartners.geojobs.repository.model.detection.Detection;
import app.bpartners.geojobs.repository.model.tiling.ZoneTilingJob;
import app.bpartners.geojobs.service.DetectionFinishedMailer;
import app.bpartners.geojobs.template.HTMLTemplateParser;
import java.util.Optional;
import java.util.function.Consumer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;

@Service
@RequiredArgsConstructor
public class ZoneTilingJobFailedService implements Consumer<ZoneTilingJobFailed> {
  private static final String ZONE_TILING_JOB_FAILED_TEMPLATE = "zone_tiling_job_failed_template";
  private final DetectionFinishedMailer mailer;
  private final DetectionRepository detectionRepository;
  private final HTMLTemplateParser htmlTemplateParser;

  @Override
  public void accept(ZoneTilingJobFailed event) {
    var failedJob = event.getFailedJob();

    var optionalDetection = detectionRepository.findByZtjId(failedJob.getId());
    StringBuilder subjectBuilder = new StringBuilder();
    if (optionalDetection.isPresent()) {
      subjectBuilder
          .append("Erreur survenue lors du traitement de la détection portant l'ID ")
          .append(optionalDetection.get().getEndToEndId());
    } else {
      subjectBuilder
          .append("Erreur survenue lors du traitement du pavage (ZTJ.id=")
          .append(failedJob.getId())
          .append(")");
    }
    mailer.accept(
        failedJob.getEmailReceiver(),
        subjectBuilder.toString(),
        getBody(optionalDetection, failedJob));
  }

  private String getBody(Optional<Detection> optionalDetection, ZoneTilingJob failedJob) {
    Context context = new Context();
    context.setVariable("detection", optionalDetection.orElse(null));
    context.setVariable("failedJob", failedJob);
    return htmlTemplateParser.apply(ZONE_TILING_JOB_FAILED_TEMPLATE, context);
  }
}
