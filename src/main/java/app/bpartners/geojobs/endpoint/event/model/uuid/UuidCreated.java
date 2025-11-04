package app.bpartners.geojobs.endpoint.event.model.uuid;

import app.bpartners.geojobs.PojaGenerated;
import app.bpartners.geojobs.endpoint.event.model.PojaEvent;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Duration;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

@PojaGenerated
@SuppressWarnings("all")
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
@Data
@EqualsAndHashCode(callSuper = false)
@ToString
public class UuidCreated extends PojaEvent {
  @JsonProperty("uuid")
  private String uuid;

  @Override
  public Duration maxConsumerDuration() {
    return Duration.ofSeconds(10);
  }

  @Override
  public Duration maxConsumerBackoffBetweenRetries() {
    return Duration.ofSeconds(30);
  }
}
