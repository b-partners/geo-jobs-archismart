package app.bpartners.geojobs.service.event;

import app.bpartners.geojobs.endpoint.event.EventProducer;
import app.bpartners.geojobs.endpoint.event.model.zone.ZoneDetectionJobFailed;
import app.bpartners.geojobs.endpoint.event.model.zone.ZoneDetectionJobStatusChanged;
import app.bpartners.geojobs.endpoint.event.model.zone.ZoneDetectionJobSucceeded;
import app.bpartners.geojobs.repository.model.detection.ZoneDetectionJob;
import app.bpartners.geojobs.service.JobFinishedMailer;
import app.bpartners.geojobs.service.StatusChangedHandler;
import java.util.List;
import java.util.function.Consumer;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@AllArgsConstructor
public class ZoneDetectionJobStatusChangedService
    implements Consumer<ZoneDetectionJobStatusChanged> {
  private final JobFinishedMailer<ZoneDetectionJob> mailer;
  private final EventProducer eventProducer;
  private final StatusChangedHandler statusChangedHandler;

  @Override
  public void accept(ZoneDetectionJobStatusChanged event) {
    var oldJob = event.getOldJob();
    var newJob = event.getNewJob();

    statusChangedHandler.handle(
        event,
        newJob.getStatus(),
        oldJob.getStatus(),
        new OnSucceededHandler(mailer, eventProducer, newJob),
        new OnFailedHandler(mailer, eventProducer, newJob));
  }

  private record OnSucceededHandler(
      JobFinishedMailer<ZoneDetectionJob> mailer, EventProducer eventProducer, ZoneDetectionJob zdj)
      implements Runnable {

    @Override
    public void run() {
      mailer.accept(zdj);
      eventProducer.accept(
          List.of(ZoneDetectionJobSucceeded.builder().succeededJobId(zdj.getId()).build()));
      log.info("Finished, mail sent, ztj=" + zdj);
    }
  }

  private record OnFailedHandler(
      JobFinishedMailer<ZoneDetectionJob> mailer, EventProducer eventProducer, ZoneDetectionJob zdj)
      implements Runnable {

    @Override
    public void run() {
      mailer.accept(zdj);
      eventProducer.accept(
          List.of(ZoneDetectionJobFailed.builder().failedJobId(zdj.getId()).build()));
      log.info("Failed to process ZDJ {}, mail sent", zdj);
    }
  }
}
