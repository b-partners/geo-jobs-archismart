package app.bpartners.geojobs.service.event;

import app.bpartners.geojobs.endpoint.event.EventProducer;
import app.bpartners.geojobs.endpoint.event.model.GeoJsonConversionAssemblyInitiated;
import app.bpartners.geojobs.endpoint.event.model.GeoJsonConversionJobStatusChanged;
import app.bpartners.geojobs.repository.DetectionRepository;
import app.bpartners.geojobs.repository.model.detection.Detection;
import app.bpartners.geojobs.repository.model.geojson.GeoJsonConversionJob;
import app.bpartners.geojobs.service.DetectionFinishedMailer;
import app.bpartners.geojobs.service.StatusChangedHandler;
import app.bpartners.geojobs.template.HTMLTemplateParser;
import java.util.List;
import java.util.function.Consumer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;

@Service
@RequiredArgsConstructor
@Slf4j
public class GeoJsonConversionJobStatusChangedService
    implements Consumer<GeoJsonConversionJobStatusChanged> {
  private static final String GEO_JSON_CONVERSION_JOB_FAILED_TEMPLATE =
      "geo_json_conversion_job_failed_template";
  private final StatusChangedHandler statusChangedHandler;
  private final EventProducer eventProducer;
  private final DetectionRepository detectionRepository;
  private final DetectionFinishedMailer mailer;
  private final HTMLTemplateParser htmlTemplateParser;

  @Override
  public void accept(GeoJsonConversionJobStatusChanged event) {
    var oldJob = event.getOldJob();
    var newJob = event.getNewJob();

    var onSucceededHandler = new OnSucceededHandler(newJob, eventProducer);
    var onFailedHandler =
        new OnFailedHandler(mailer, detectionRepository, newJob, htmlTemplateParser);

    statusChangedHandler.handle(
        event, newJob.getStatus(), oldJob.getStatus(), onSucceededHandler, onFailedHandler);
  }

  private record OnSucceededHandler(GeoJsonConversionJob newJob, EventProducer eventProducer)
      implements Runnable {

    @Override
    public void run() {
      eventProducer.accept(
          List.of(
              GeoJsonConversionAssemblyInitiated.builder()
                  .geoJsonConversionJobId(newJob.getId())
                  .build()));
      log.info("GeoJsonConversionJob(id=" + newJob.getId() + ") finished, assembly initiated");
    }
  }

  private record OnFailedHandler(
      DetectionFinishedMailer mailer,
      DetectionRepository detectionRepository,
      GeoJsonConversionJob failedJob,
      HTMLTemplateParser htmlTemplateParser)
      implements Runnable {

    @Override
    public void run() {
      var zoneDetectionJobId = failedJob.getZoneDetectionJobId();
      var detection = detectionRepository.findByZdjId(zoneDetectionJobId).orElse(null);
      var emailSubject = getEmailSubject(detection);
      var emailReceiver =
          detection == null ? failedJob.getEmailReceiver() : detection.getEmailReceiver();
      var emailBody = getEmailBody(detection);

      mailer.accept(emailReceiver, emailSubject, emailBody);
    }

    private String getEmailSubject(Detection detection) {
      StringBuilder subjectBuilder = new StringBuilder();
      if (detection != null) {
        subjectBuilder
            .append("Erreur survenue lors du traitement de la détection portant l'ID ")
            .append(detection.getEndToEndId());
      } else {
        subjectBuilder
            .append("Erreur survenue lors du traitement de la conversion en geojson (id=")
            .append(failedJob.getId())
            .append(")");
      }
      return subjectBuilder.toString();
    }

    private String getEmailBody(Detection detection) {
      Context context = new Context();
      context.setVariable("detection", detection);
      context.setVariable("failedJob", failedJob);
      return htmlTemplateParser.apply(GEO_JSON_CONVERSION_JOB_FAILED_TEMPLATE, context);
    }
  }
}
