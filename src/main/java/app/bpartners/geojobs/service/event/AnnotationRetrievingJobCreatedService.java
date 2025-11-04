package app.bpartners.geojobs.service.event;

import app.bpartners.geojobs.endpoint.event.model.annotation.AnnotationRetrievingJobCreated;
import app.bpartners.geojobs.service.AnnotationRetrievingJobService;
import java.util.function.Consumer;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
@Slf4j
public class AnnotationRetrievingJobCreatedService
    implements Consumer<AnnotationRetrievingJobCreated> {
  private final AnnotationRetrievingJobService annotationRetrievingJobService;

  @Override
  public void accept(AnnotationRetrievingJobCreated event) {
    annotationRetrievingJobService.fireTasks(event.getRetrievingJob().getId());
  }
}
