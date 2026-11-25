package app.bpartners.geojobs.endpoint.event.model;

import java.time.Duration;
import lombok.*;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@EqualsAndHashCode(callSuper = false)
@ToString
public class DetectionTrackingRegistrationRequested extends PojaEvent {
  private String detectionIdentifier;
  private String failureId;

  public DetectionTrackingRegistrationRequested(String detectionIdentifier) {
    this(detectionIdentifier, null);
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
