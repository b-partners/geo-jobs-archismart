package app.bpartners.geojobs.endpoint.event.model.status;

import app.bpartners.geojobs.endpoint.event.model.PojaEvent;
import java.time.Duration;
import lombok.*;

@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
@Data
@EqualsAndHashCode(callSuper = false)
@ToString
public class ZDJParcelsStatusRecomputingSubmitted extends PojaEvent {
  private String zoneDetectionJobId;

  @Override
  public Duration maxConsumerDuration() {
    return Duration.ofMinutes(5);
  }

  @Override
  public Duration maxConsumerBackoffBetweenRetries() {
    return Duration.ofMinutes(1);
  }
}
