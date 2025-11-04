package app.bpartners.geojobs.service.event;

import app.bpartners.geojobs.endpoint.event.EventProducer;
import app.bpartners.geojobs.endpoint.event.model.annotation.AnnotationRetrievingJobStatusChanged;
import app.bpartners.geojobs.endpoint.event.model.status.HumanZDJStatusRecomputingSubmitted;
import app.bpartners.geojobs.repository.model.annotation.AnnotationRetrievingJob;
import app.bpartners.geojobs.service.StatusChangedHandler;
import java.util.List;
import java.util.function.Consumer;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@AllArgsConstructor
public class AnnotationRetrievingJobStatusChangedService
    implements Consumer<AnnotationRetrievingJobStatusChanged> {
  private final EventProducer eventProducer;
  private final StatusChangedHandler statusChangedHandler;

  @Override
  public void accept(AnnotationRetrievingJobStatusChanged event) {
    var oldJob = event.getOldJob();
    var newJob = event.getNewJob();

    OnFinishedHandler onFinishedHandler = new OnFinishedHandler(eventProducer, newJob);

    statusChangedHandler.handle(
        event, newJob.getStatus(), oldJob.getStatus(), onFinishedHandler, onFinishedHandler);
  }

  private record OnFinishedHandler(EventProducer eventProducer, AnnotationRetrievingJob newJob)
      implements Runnable {

    @Override
    public void run() {
      String detectionJobId = newJob.getDetectionJobId();

      eventProducer.accept(List.of(new HumanZDJStatusRecomputingSubmitted(detectionJobId)));

      log.info(
          "AnnotationRetrievedJob (id"
              + newJob.getId()
              + ") finished with status "
              + newJob.getStatus());
    }
  }
}
