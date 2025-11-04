package app.bpartners.geojobs.service.event;

import static app.bpartners.gen.annotator.endpoint.rest.model.JobStatus.STARTED;
import static app.bpartners.geojobs.job.model.Status.HealthStatus.UNKNOWN;
import static app.bpartners.geojobs.job.model.Status.ProgressionStatus.PROCESSING;
import static app.bpartners.geojobs.repository.model.GeoJobType.ANNOTATION_DELIVERY;
import static java.time.Instant.now;
import static java.util.UUID.randomUUID;

import app.bpartners.geojobs.endpoint.event.model.annotation.AnnotationDeliveryJobStatusChanged;
import app.bpartners.geojobs.job.model.JobStatus;
import app.bpartners.geojobs.mail.Email;
import app.bpartners.geojobs.mail.Mailer;
import app.bpartners.geojobs.repository.model.annotation.AnnotationDeliveryJob;
import app.bpartners.geojobs.service.StatusChangedHandler;
import app.bpartners.geojobs.service.annotator.AnnotationService;
import app.bpartners.geojobs.service.detection.ZoneDetectionJobService;
import jakarta.mail.internet.AddressException;
import jakarta.mail.internet.InternetAddress;
import java.io.File;
import java.util.List;
import java.util.function.Consumer;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@AllArgsConstructor
public class AnnotationDeliveryJobStatusChangedService
    implements Consumer<AnnotationDeliveryJobStatusChanged> {
  private final StatusChangedHandler statusChangedHandler;
  private final AnnotationService annotationService;
  private final ZoneDetectionJobService zoneDetectionJobService;
  private final Mailer mailer;

  @Override
  public void accept(AnnotationDeliveryJobStatusChanged event) {
    var oldJob = event.getOldJob();
    var newJob = event.getNewJob();

    OnFinishedHandler onFinishedHandler =
        new OnFinishedHandler(zoneDetectionJobService, annotationService, mailer, newJob);

    statusChangedHandler.handle(
        event, newJob.getStatus(), oldJob.getStatus(), onFinishedHandler, onFinishedHandler);
  }

  private record OnFinishedHandler(
      ZoneDetectionJobService zoneDetectionJobService,
      AnnotationService annotationService,
      Mailer mailer,
      AnnotationDeliveryJob newJob)
      implements Runnable {

    @Override
    @SneakyThrows
    public void run() {
      var detectionJobId = newJob.getDetectionJobId();
      var annotationJobId = newJob.getAnnotationJobId();
      var annotationJobName = newJob.getAnnotationJobName();
      var labels = newJob.getLabels();

      annotationService.saveAnnotationJob(
          detectionJobId, annotationJobId, annotationJobName, labels, STARTED);
      updateZDJStatus(detectionJobId);
      notifyAdminByEmail(newJob);

      log.info(
          "Annotation Delivery Job (id"
              + newJob.getId()
              + ") finished with status "
              + newJob.getStatus());
    }

    private void notifyAdminByEmail(AnnotationDeliveryJob deliveryJob) throws AddressException {
      List<InternetAddress> cc = List.of();
      List<InternetAddress> bcc = List.of();
      String subject =
          "AnnotationJob(id="
              + deliveryJob.getAnnotationJobId()
              + ",nom="
              + deliveryJob.getAnnotationJobName()
              + ") disponible sur annotator";
      String htmlBody = "";
      List<File> attachments = List.of();
      mailer.accept(
          new Email(
              new InternetAddress("tech@birdia.fr"), cc, bcc, subject, htmlBody, attachments));
    }

    private void updateZDJStatus(String detectionJobId) {
      var humanZDJ = zoneDetectionJobService.getHumanZdjFromZdjId(detectionJobId);
      humanZDJ.hasNewStatus(
          JobStatus.builder()
              .id(randomUUID().toString())
              .jobId(humanZDJ.getId())
              .jobType(ANNOTATION_DELIVERY)
              .progression(PROCESSING)
              .health(UNKNOWN)
              .creationDatetime(now())
              .build());
      zoneDetectionJobService.save(humanZDJ);
    }
  }
}
