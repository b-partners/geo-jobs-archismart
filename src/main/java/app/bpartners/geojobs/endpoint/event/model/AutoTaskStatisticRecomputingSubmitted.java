package app.bpartners.geojobs.endpoint.event.model;

import static app.bpartners.geojobs.endpoint.event.EventStack.EVENT_STACK_2;

import app.bpartners.geojobs.endpoint.event.EventStack;
import java.time.Duration;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
@Data
@EqualsAndHashCode(callSuper = false)
@ToString
public class AutoTaskStatisticRecomputingSubmitted extends PojaEvent {
  private static final long MAX_CONSUMER_DURATION_VALUE = 30L;
  private static final long DEFAULT_BACK_OFF_VALUE = 60L;
  protected String jobId;

  @Override
  public Duration eventHandlerInitMaxDuration() {
    return Duration.ofSeconds(0);
  }

  @Override
  public Duration maxConsumerDuration() {
    return Duration.ofSeconds(MAX_CONSUMER_DURATION_VALUE);
  }

  @Override
  public Duration maxConsumerBackoffBetweenRetries() {
    return Duration.ofSeconds(DEFAULT_BACK_OFF_VALUE * (int) Math.pow(2, getAttemptNb()));
  }

  @Override
  public EventStack getEventStack() {
    return EVENT_STACK_2;
  }
}
