package app.bpartners.geojobs.service.event;

import app.bpartners.geojobs.endpoint.event.model.zone.ZoneDetectionJobFailed;
import app.bpartners.geojobs.repository.DetectionRepository;
import app.bpartners.geojobs.repository.model.detection.Detection;
import app.bpartners.geojobs.repository.model.detection.ZoneDetectionJob;
import app.bpartners.geojobs.service.DetectionFinishedMailer;
import app.bpartners.geojobs.service.detection.ZoneDetectionJobService;
import app.bpartners.geojobs.template.HTMLTemplateParser;
import java.util.Optional;
import java.util.function.Consumer;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;

@Slf4j
@Service
@AllArgsConstructor
public class ZoneDetectionJobFailedService implements Consumer<ZoneDetectionJobFailed> {
  private static final String ZONE_DETECTION_JOB_FAILED_MAIL_TEMPLATE =
      "zone_detection_job_failed_template";
  private static final String ADMIN_EMAIL = "tech@birdia.fr";
  private final DetectionFinishedMailer mailer;
  private final ZoneDetectionJobService zoneDetectionJobService;
  private final DetectionRepository detectionRepository;
  private final HTMLTemplateParser htmlTemplateParser;

  @Override
  public void accept(ZoneDetectionJobFailed event) {
    var jobId = event.getFailedJobId();
    var zoneDetectionJob = zoneDetectionJobService.findById(jobId);
    var optionalDetection = detectionRepository.findByZdjId(zoneDetectionJob.getId());
    StringBuilder subjectBuilder = new StringBuilder();
    if (optionalDetection.isPresent()) {
      subjectBuilder
          .append("Erreur lors du traitement de la détection portant l'ID ")
          .append(optionalDetection.get().getEndToEndId());
    } else {
      subjectBuilder
          .append("Erreur lors du traitement de la détection machine (ZDJ.id=")
          .append(zoneDetectionJob.getId())
          .append(")");
    }

    mailer.accept(
        optionalDetection.isPresent() ? zoneDetectionJob.getEmailReceiver() : ADMIN_EMAIL,
        subjectBuilder.toString(),
        getBody(optionalDetection, zoneDetectionJob));
  }

  private String getBody(Optional<Detection> optionalDetection, ZoneDetectionJob failedJob) {
    Context context = new Context();
    context.setVariable("detection", optionalDetection.orElse(null));
    context.setVariable("failedJob", failedJob);
    return htmlTemplateParser.apply(ZONE_DETECTION_JOB_FAILED_MAIL_TEMPLATE, context);
  }
}
