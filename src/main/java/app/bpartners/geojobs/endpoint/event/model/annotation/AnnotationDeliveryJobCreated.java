package app.bpartners.geojobs.endpoint.event.model.annotation;

import static app.bpartners.geojobs.endpoint.event.EventStack.EVENT_STACK_2;

import app.bpartners.geojobs.endpoint.event.EventStack;
import app.bpartners.geojobs.endpoint.event.model.PojaEvent;
import app.bpartners.geojobs.repository.model.annotation.AnnotationDeliveryJob;
import java.time.Duration;
import lombok.*;

@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
@Data
@EqualsAndHashCode(callSuper = false)
@ToString
public class AnnotationDeliveryJobCreated extends PojaEvent {
  private AnnotationDeliveryJob deliveryJob;

  @Override
  public Duration maxConsumerDuration() {
    return Duration.ofMinutes(3L);
  }

  @Override
  public Duration maxConsumerBackoffBetweenRetries() {
    return Duration.ofMinutes(3L);
  }

  @Override
  public EventStack getEventStack() {
    return EVENT_STACK_2;
  }
}
