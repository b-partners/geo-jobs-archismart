package app.bpartners.geojobs.service.event;

import app.bpartners.geojobs.endpoint.event.model.annotation.AnnotationDeliveryJobStatusRecomputingSubmitted;
import java.util.function.Consumer;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class AnnotationDeliveryJobStatusRecomputingSubmittedService
    implements Consumer<AnnotationDeliveryJobStatusRecomputingSubmitted> {
  private final AnnotationDeliveryJobStatusRecomputingSubmittedBean deliveryJobStatusRecomputing;

  @Override
  public void accept(AnnotationDeliveryJobStatusRecomputingSubmitted submitted) {
    deliveryJobStatusRecomputing.accept(submitted);
  }
}
