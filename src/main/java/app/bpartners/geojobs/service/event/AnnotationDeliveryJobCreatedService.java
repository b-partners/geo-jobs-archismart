package app.bpartners.geojobs.service.event;

import static app.bpartners.gen.annotator.endpoint.rest.model.JobStatus.PENDING;

import app.bpartners.geojobs.endpoint.event.model.annotation.AnnotationDeliveryJobCreated;
import app.bpartners.geojobs.service.AnnotationDeliveryJobService;
import app.bpartners.geojobs.service.annotator.AnnotationService;
import java.util.function.Consumer;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@AllArgsConstructor
public class AnnotationDeliveryJobCreatedService implements Consumer<AnnotationDeliveryJobCreated> {
  private final AnnotationService annotationService;
  private final AnnotationDeliveryJobService annotationDeliveryJobService;

  @Override
  public void accept(AnnotationDeliveryJobCreated event) {
    var deliveryJob = event.getDeliveryJob();
    var detectionJobId = deliveryJob.getDetectionJobId();
    var annotationJobId = deliveryJob.getAnnotationJobId();
    var annotationJobName = deliveryJob.getAnnotationJobName();
    var labels = deliveryJob.getLabels();

    annotationService.saveAnnotationJob(
        detectionJobId, annotationJobId, annotationJobName, labels, PENDING);

    annotationDeliveryJobService.fireTasks(deliveryJob);
  }
}
