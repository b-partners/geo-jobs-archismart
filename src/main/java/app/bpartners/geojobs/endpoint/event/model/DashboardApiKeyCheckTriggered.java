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
  private String communityAuthorizationId;
  private String email;
  private String dashboardApiKey;

  // position in the ordered list of authorizations to check, the lane then continues with
  // index + LANES. null means a standalone check
  private Integer index;

  // concurrent checks, kept under the dashboard api reserved concurrency to avoid 429s
  public static final int LANES = 10;

  public static DashboardApiKeyCheckTriggered of(CommunityAuthorization authorization, int index) {
    return DashboardApiKeyCheckTriggered.builder()
        .communityAuthorizationId(authorization.getId())
        .email(authorization.getEmail())
        .dashboardApiKey(authorization.getDashboardApiKey())
        .index(index)
        .build();
  }

  @Override
  public Duration maxConsumerDuration() {
    return Duration.ofMinutes(3L);
  }

  @Override
  public Duration maxConsumerBackoffBetweenRetries() {
    return Duration.ofMinutes(1L);
  }
}
