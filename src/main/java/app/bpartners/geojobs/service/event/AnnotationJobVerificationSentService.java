package app.bpartners.geojobs.service.event;

import app.bpartners.geojobs.endpoint.event.model.annotation.AnnotationJobVerificationSent;
import java.util.function.Consumer;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
@Slf4j
public class AnnotationJobVerificationSentService
    implements Consumer<AnnotationJobVerificationSent> {
  private final AnnotationRetriever annotationRetriever;

  @Override
  public void accept(AnnotationJobVerificationSent annotationJobVerificationSent) {
    var humanZdjId = annotationJobVerificationSent.getHumanZdjId();

    annotationRetriever.accept(humanZdjId);
  }
}
