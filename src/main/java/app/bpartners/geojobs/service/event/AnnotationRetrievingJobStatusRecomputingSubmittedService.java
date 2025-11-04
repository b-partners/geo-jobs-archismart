package app.bpartners.geojobs.service.event;

import app.bpartners.geojobs.endpoint.event.model.annotation.AnnotationRetrievingJobStatusRecomputingSubmitted;
import java.util.function.Consumer;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class AnnotationRetrievingJobStatusRecomputingSubmittedService
    implements Consumer<AnnotationRetrievingJobStatusRecomputingSubmitted> {
  private final AnnotationRetrievingJobStatusRecomputingSubmittedBean retrievedJobStatusService;

  @Override
  public void accept(AnnotationRetrievingJobStatusRecomputingSubmitted submitted) {
    retrievedJobStatusService.accept(submitted);
  }
}
