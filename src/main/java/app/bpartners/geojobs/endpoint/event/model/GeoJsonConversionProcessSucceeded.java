package app.bpartners.geojobs.endpoint.event.model;

import app.bpartners.geojobs.repository.model.detection.Detection;
import java.time.Duration;
import lombok.*;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@EqualsAndHashCode(callSuper = false)
@ToString
public class GeoJsonConversionProcessSucceeded extends PojaEvent {
  private Detection detection;

  @Override
  public Duration maxConsumerDuration() {
    return Duration.ofSeconds(30);
  }

  @Override
  public Duration maxConsumerBackoffBetweenRetries() {
    return Duration.ofSeconds(60);
  }
}
