package app.bpartners.geojobs.endpoint.event.model.annotation;

import static app.bpartners.geojobs.endpoint.event.EventStack.EVENT_STACK_2;

import app.bpartners.geojobs.endpoint.event.EventStack;
import app.bpartners.geojobs.endpoint.event.model.PojaEvent;
import app.bpartners.geojobs.repository.model.detection.DetectableObjectConfiguration;
import java.time.Duration;
import java.util.List;
import lombok.*;

@AllArgsConstructor
@Data
@EqualsAndHashCode(callSuper = false)
@ToString
public abstract class AnnotationModelDeliveryRequested extends PojaEvent {
  protected String zoneDetectionJobId;
  protected String annotationJobId;
  protected Double minimumConfidenceForDelivery;
  protected List<DetectableObjectConfiguration> detectableObjectConfigurations;

  @Override
  public Duration maxConsumerDuration() {
    return Duration.ofMinutes(5L);
  }

  @Override
  public Duration maxConsumerBackoffBetweenRetries() {
    return Duration.ofMinutes(1L);
  }

  @Override
  public EventStack getEventStack() {
    return EVENT_STACK_2;
  }

  public abstract AnnotationModelDeliveryType getAnnotationModelDeliveryType();
}
