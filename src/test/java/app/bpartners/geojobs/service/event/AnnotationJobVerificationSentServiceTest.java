package app.bpartners.geojobs.service.event;

import static org.mockito.Mockito.*;

import app.bpartners.geojobs.endpoint.event.model.annotation.AnnotationJobVerificationSent;
import org.junit.jupiter.api.Test;

class AnnotationJobVerificationSentServiceTest {
  AnnotationRetriever annotationRetrieverMock = mock();
  AnnotationJobVerificationSentService subject =
      new AnnotationJobVerificationSentService(annotationRetrieverMock);

  @Test
  void process_ok() {
    String detectionJobId = "detectionJobId";

    subject.accept(AnnotationJobVerificationSent.builder().humanZdjId(detectionJobId).build());

    verify(annotationRetrieverMock, times(1)).accept(detectionJobId);
  }
}
