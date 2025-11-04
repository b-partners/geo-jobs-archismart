package app.bpartners.geojobs.service.event;

import app.bpartners.geojobs.endpoint.event.model.annotation.AnnotationWithoutDetectedObjectDeliveryRequested;
import java.util.function.Consumer;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@AllArgsConstructor
@Service
@Slf4j
public class AnnotationWithoutDetectedObjectDeliveryRequestedService
    implements Consumer<AnnotationWithoutDetectedObjectDeliveryRequested> {
  private final AnnotationModelDeliveryRequestedService<
          AnnotationWithoutDetectedObjectDeliveryRequested>
      service;

  @Override
  public void accept(AnnotationWithoutDetectedObjectDeliveryRequested event) {
    service.accept(event);
  }
}
