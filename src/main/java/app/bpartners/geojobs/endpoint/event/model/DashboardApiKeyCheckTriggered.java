package app.bpartners.geojobs.endpoint.event.model;

import app.bpartners.geojobs.repository.model.community.CommunityAuthorization;
import java.time.Duration;
import lombok.*;

@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
@Data
@EqualsAndHashCode(callSuper = false)
@ToString
public class DashboardApiKeyCheckTriggered extends PojaEvent {
  private CommunityAuthorization communityAuthorization;

  @Override
  public Duration maxConsumerDuration() {
    return Duration.ofMinutes(3L);
  }

  @Override
  public Duration maxConsumerBackoffBetweenRetries() {
    return Duration.ofMinutes(1L);
  }
}
