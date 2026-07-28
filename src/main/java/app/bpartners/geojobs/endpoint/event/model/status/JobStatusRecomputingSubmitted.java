package app.bpartners.geojobs.endpoint.event.model.status;

import static app.bpartners.geojobs.endpoint.event.EventStack.EVENT_STACK_2;

import app.bpartners.geojobs.endpoint.event.EventStack;
import app.bpartners.geojobs.endpoint.event.model.PojaEvent;
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
public class JobStatusRecomputingSubmitted extends PojaEvent {
  protected String jobId;
  protected long initialConsumerBackoffBetweenRetriesInSeconds;

  @Override
  public Duration eventHandlerInitMaxDuration() {
    // Covers the Spring Boot cold start inside the handler (~25-40s observed on the workers)
    // so the message stays invisible until init+consume finish. Kept as tight as possible:
    // too low -> premature SQS redelivery mid-boot; too high -> failed messages retry slower.
    return Duration.ofSeconds(45);
  }

  @Override
  public Duration maxConsumerDuration() {
    return Duration.ofSeconds(30);
  }

  @Override
  public Duration maxConsumerBackoffBetweenRetries() {
    // u_n = u_0 * q^n
    // u_0: maxConsumerBackoffBetweenRetriesDurationValue
    // q: 2
    // n: attemptNb()
    return Duration.ofSeconds(
        initialConsumerBackoffBetweenRetriesInSeconds * (long) Math.pow(2, getAttemptNb()));
  }

  @Override
  public EventStack getEventStack() {
    return EVENT_STACK_2;
  }
}
