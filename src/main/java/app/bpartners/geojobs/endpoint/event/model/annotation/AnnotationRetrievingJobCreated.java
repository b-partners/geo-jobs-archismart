package app.bpartners.geojobs.endpoint.event.model.annotation;

import app.bpartners.geojobs.endpoint.event.model.PojaEvent;
import app.bpartners.geojobs.repository.model.annotation.AnnotationRetrievingJob;
import java.time.Duration;
import lombok.*;

@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
@Data
@EqualsAndHashCode(callSuper = false)
@ToString
public class AnnotationRetrievingJobCreated extends PojaEvent {
  private AnnotationRetrievingJob retrievingJob;

  @Override
  public Duration maxConsumerDuration() {
    return Duration.ofMinutes(3L);
  }

  @Override
  public Duration maxConsumerBackoffBetweenRetries() {
    return Duration.ofMinutes(3L);
  }
}
