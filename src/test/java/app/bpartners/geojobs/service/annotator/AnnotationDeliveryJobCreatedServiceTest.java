package app.bpartners.geojobs.service.annotator;

import static app.bpartners.gen.annotator.endpoint.rest.model.JobStatus.PENDING;
import static org.mockito.Mockito.*;

import app.bpartners.geojobs.endpoint.event.model.annotation.AnnotationDeliveryJobCreated;
import app.bpartners.geojobs.repository.model.annotation.AnnotationDeliveryJob;
import app.bpartners.geojobs.service.AnnotationDeliveryJobService;
import app.bpartners.geojobs.service.event.AnnotationDeliveryJobCreatedService;
import org.junit.jupiter.api.Test;

class AnnotationDeliveryJobCreatedServiceTest {
  AnnotationService annotationServiceMock = mock();
  AnnotationDeliveryJobService deliveryJobServiceMock = mock();
  AnnotationDeliveryJobCreatedService subject =
      new AnnotationDeliveryJobCreatedService(annotationServiceMock, deliveryJobServiceMock);

  @Test
  void accept_ok() {
    var deliveryJob = new AnnotationDeliveryJob();

    subject.accept(AnnotationDeliveryJobCreated.builder().deliveryJob(deliveryJob).build());

    verify(deliveryJobServiceMock, only()).fireTasks(deliveryJob);
    verify(annotationServiceMock, only())
        .saveAnnotationJob(
            deliveryJob.getDetectionJobId(),
            deliveryJob.getAnnotationJobId(),
            deliveryJob.getAnnotationJobName(),
            deliveryJob.getLabels(),
            PENDING);
  }
}
