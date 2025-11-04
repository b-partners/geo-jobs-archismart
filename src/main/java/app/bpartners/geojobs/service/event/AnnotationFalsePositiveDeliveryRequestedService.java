package app.bpartners.geojobs.service.event;

import app.bpartners.geojobs.endpoint.event.model.annotation.AnnotationFalsePositiveDeliveryRequested;
import java.util.function.Consumer;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@AllArgsConstructor
@Service
@Slf4j
public class AnnotationFalsePositiveDeliveryRequestedService
    implements Consumer<AnnotationFalsePositiveDeliveryRequested> {
  private final AnnotationModelDeliveryRequestedService<AnnotationFalsePositiveDeliveryRequested>
      service;

  @Override
  public void accept(AnnotationFalsePositiveDeliveryRequested event) {
    service.accept(event);
  }
}
