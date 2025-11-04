package app.bpartners.geojobs.service.event;

import app.bpartners.geojobs.endpoint.event.model.annotation.AnnotationTruePositiveDeliveryRequested;
import java.util.function.Consumer;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
@Slf4j
public class AnnotationTruePositiveDeliveryRequestedService
    implements Consumer<AnnotationTruePositiveDeliveryRequested> {
  private final AnnotationModelDeliveryRequestedService<AnnotationTruePositiveDeliveryRequested>
      service;

  @Override
  public void accept(AnnotationTruePositiveDeliveryRequested event) {
    service.accept(event);
  }
}
