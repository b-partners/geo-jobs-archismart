package app.bpartners.geojobs.endpoint.event.model.annotation;

import app.bpartners.geojobs.endpoint.event.model.PojaEvent;
import app.bpartners.geojobs.repository.model.annotation.AnnotationRetrievingTask;
import java.time.Duration;
import lombok.*;

@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
@Data
@EqualsAndHashCode(callSuper = false)
@ToString
public class AnnotationRetrievingTaskSucceeded extends PojaEvent {
  private AnnotationRetrievingTask annotationRetrievingTask;

  @Override
  public Duration maxConsumerDuration() {
    return Duration.ofMinutes(10);
  }

  @Override
  public Duration maxConsumerBackoffBetweenRetries() {
    return Duration.ofMinutes(1);
  }
}
