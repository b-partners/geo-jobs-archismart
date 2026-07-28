package app.bpartners.geojobs.endpoint.event.model;

import app.bpartners.geojobs.job.model.Task;
import java.time.Duration;
import lombok.*;

@Getter
@Setter
@EqualsAndHashCode(callSuper = false)
@NoArgsConstructor
@ToString
public class TaskCreated<T extends Task> extends PojaEvent {

  protected T task;

  protected TaskCreated(T task) {
    this.task = task;
  }

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
    return Duration.ofSeconds(60);
  }
}
