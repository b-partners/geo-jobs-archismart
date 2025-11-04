package app.bpartners.geojobs.endpoint.event.model.zone;

import app.bpartners.geojobs.endpoint.event.model.PojaEvent;
import java.time.Duration;
import lombok.*;

@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
@Data
@EqualsAndHashCode(callSuper = false)
@ToString
public class ZoneDetectionJobFailed extends PojaEvent {
  private String failedJobId;

  @Override
  public Duration maxConsumerDuration() {
    return Duration.ofMinutes(1);
  }

  @Override
  public Duration maxConsumerBackoffBetweenRetries() {
    return Duration.ofMinutes(1);
  }
}
