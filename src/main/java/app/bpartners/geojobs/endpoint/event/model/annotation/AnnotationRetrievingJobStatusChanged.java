package app.bpartners.geojobs.endpoint.event.model.annotation;

import app.bpartners.geojobs.endpoint.event.model.PojaEvent;
import app.bpartners.geojobs.repository.model.annotation.AnnotationRetrievingJob;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Duration;
import lombok.*;

@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
@Data
@EqualsAndHashCode(callSuper = false)
@ToString
public class AnnotationRetrievingJobStatusChanged extends PojaEvent {
  @JsonProperty("oldJob")
  private AnnotationRetrievingJob oldJob;

  @JsonProperty("newJob")
  private AnnotationRetrievingJob newJob;

  @Override
  public Duration maxConsumerDuration() {
    return Duration.ofMinutes(1L);
  }

  @Override
  public Duration maxConsumerBackoffBetweenRetries() {
    return Duration.ofMinutes(5L);
  }
}
