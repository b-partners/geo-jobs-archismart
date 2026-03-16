package app.bpartners.geojobs.endpoint.event.model;

import java.time.Duration;
import lombok.*;

@NoArgsConstructor
@Builder(toBuilder = true)
@Data
@EqualsAndHashCode(callSuper = false)
@ToString
public class AllDashboardApiKeyCheckTriggered extends PojaEvent {
  @Override
  public Duration maxConsumerDuration() {
    return Duration.ofMinutes(3L);
  }

  @Override
  public Duration maxConsumerBackoffBetweenRetries() {
    return Duration.ofMinutes(1L);
  }
}
