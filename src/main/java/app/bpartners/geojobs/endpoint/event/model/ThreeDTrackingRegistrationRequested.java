package app.bpartners.geojobs.endpoint.event.model;

import java.time.Duration;
import lombok.*;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@EqualsAndHashCode(callSuper = false)
@ToString
public class ThreeDTrackingRegistrationRequested extends PojaEvent {
  private String requestId;
  private String communityOwnerId;
  private String failureId;

  public ThreeDTrackingRegistrationRequested(String requestId, String communityOwnerId) {
    this(requestId, communityOwnerId, null);
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
