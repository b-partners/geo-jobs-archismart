package app.bpartners.geojobs.endpoint.event.model.annotation;

import app.bpartners.geojobs.endpoint.event.model.PojaEvent;
import app.bpartners.geojobs.repository.model.annotation.AnnotationDeliveryJob;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Duration;
import lombok.*;

@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
@Data
@EqualsAndHashCode(callSuper = false)
@ToString
public class AnnotationDeliveryJobStatusChanged extends PojaEvent {
  @JsonProperty("oldJob")
  private AnnotationDeliveryJob oldJob;

  @JsonProperty("newJob")
  private AnnotationDeliveryJob newJob;

  @Override
  public Duration maxConsumerDuration() {
    return Duration.ofMinutes(1L);
  }

  @Override
  public Duration maxConsumerBackoffBetweenRetries() {
    return Duration.ofMinutes(5L);
  }
}
