package app.bpartners.geojobs.endpoint.event.model;

import app.bpartners.geojobs.repository.model.geojson.GeoJsonConversionJob;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Duration;
import javax.annotation.processing.Generated;
import lombok.*;

@Generated("EventBridge")
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
@Data
@EqualsAndHashCode
@ToString
public class GeoJsonConversionJobStatusChanged extends PojaEvent {
  @JsonProperty("oldJob")
  private GeoJsonConversionJob oldJob;

  @JsonProperty("newJob")
  private GeoJsonConversionJob newJob;

  @Override
  public Duration maxConsumerDuration() {
    return Duration.ofMinutes(1);
  }

  @Override
  public Duration maxConsumerBackoffBetweenRetries() {
    return Duration.ofMinutes(1);
  }
}
