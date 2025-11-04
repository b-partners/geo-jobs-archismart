package app.bpartners.geojobs.endpoint.event.model;

import static app.bpartners.geojobs.endpoint.event.EventStack.EVENT_STACK_2;

import app.bpartners.geojobs.endpoint.event.EventStack;
import app.bpartners.geojobs.repository.model.DetectionAddressConversionJob;
import java.time.Duration;
import lombok.*;

@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
@Data
@EqualsAndHashCode(callSuper = false)
@ToString
public class DetectionAddressConversionJobFailed extends PojaEvent {
  private DetectionAddressConversionJob job;

  @Override
  public Duration maxConsumerDuration() {
    return Duration.ofMinutes(3L);
  }

  @Override
  public Duration maxConsumerBackoffBetweenRetries() {
    return Duration.ofMinutes(1L);
  }

  @Override
  public EventStack getEventStack() {
    return EVENT_STACK_2;
  }
}
