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
    return Duration.ofSeconds(0);
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
