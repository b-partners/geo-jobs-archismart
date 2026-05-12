package app.bpartners.geojobs.endpoint.event.model;

import app.bpartners.geojobs.endpoint.rest.model.Point;
import app.bpartners.geojobs.model.lidar.LidarProcessorType;
import java.time.Duration;
import java.util.List;
import lombok.*;

@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
@Data
@EqualsAndHashCode(callSuper = false)
@ToString
public class ThreeDMultipleAddressRequested extends PojaEvent {
  private String requestIdentifier;
  private String communityOwnerId;
  private List<String> addresses;
  private List<Point> points;
  private LidarProcessorType lidarProcessorType;

  @Override
  public Duration maxConsumerDuration() {
    return Duration.ofSeconds(120L);
  }

  @Override
  public Duration maxConsumerBackoffBetweenRetries() {
    return Duration.ofSeconds(60L);
  }
}
