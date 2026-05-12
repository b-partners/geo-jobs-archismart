package app.bpartners.geojobs.endpoint.event.model;

import static app.bpartners.geojobs.endpoint.event.EventStack.EVENT_STACK_2;

import app.bpartners.geojobs.endpoint.event.EventStack;
import app.bpartners.geojobs.endpoint.rest.model.Feature;
import java.time.Duration;
import lombok.*;

@NoArgsConstructor
@Builder(toBuilder = true)
@Data
@EqualsAndHashCode(callSuper = false)
@ToString
public class FeatureWithDetectionPropertiesRequested extends PojaEvent {
  private String detectionIdentifier;
  private Feature feature;

  public FeatureWithDetectionPropertiesRequested(String detectionIdentifier, Feature feature) {
    this.detectionIdentifier = detectionIdentifier;
    this.feature = feature;
  }

  @Override
  public Duration maxConsumerDuration() {
    return Duration.ofSeconds(60L);
  }

  @Override
  public Duration maxConsumerBackoffBetweenRetries() {
    return Duration.ofSeconds(30L);
  }

  @Override
  public EventStack getEventStack() {
    return EVENT_STACK_2;
  }
}
