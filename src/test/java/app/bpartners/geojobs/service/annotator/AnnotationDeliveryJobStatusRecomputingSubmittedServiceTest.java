package app.bpartners.geojobs.service.annotator;

import static org.mockito.Mockito.*;

import app.bpartners.geojobs.endpoint.event.model.annotation.AnnotationDeliveryJobStatusRecomputingSubmitted;
import app.bpartners.geojobs.service.event.AnnotationDeliveryJobStatusRecomputingSubmittedBean;
import app.bpartners.geojobs.service.event.AnnotationDeliveryJobStatusRecomputingSubmittedService;
import org.junit.jupiter.api.Test;

class AnnotationDeliveryJobStatusRecomputingSubmittedServiceTest {
  AnnotationDeliveryJobStatusRecomputingSubmittedBean deliveryJobStatusRecomputingMock = mock();
  AnnotationDeliveryJobStatusRecomputingSubmittedService subject =
      new AnnotationDeliveryJobStatusRecomputingSubmittedService(deliveryJobStatusRecomputingMock);

  @Test
  void accept_ok() {
    String jobId = "jobId";
    AnnotationDeliveryJobStatusRecomputingSubmitted event =
        new AnnotationDeliveryJobStatusRecomputingSubmitted(jobId);

    subject.accept(event);

    verify(deliveryJobStatusRecomputingMock, only()).accept(event);
  }
}
