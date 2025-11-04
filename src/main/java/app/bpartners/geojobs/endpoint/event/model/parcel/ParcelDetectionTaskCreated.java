package app.bpartners.geojobs.endpoint.event.model.parcel;

import app.bpartners.geojobs.endpoint.event.model.TaskCreated;
import app.bpartners.geojobs.repository.model.detection.ParcelDetectionTask;
import java.time.Duration;
import lombok.*;

@Getter
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@ToString
public class ParcelDetectionTaskCreated extends TaskCreated<ParcelDetectionTask> {

  public ParcelDetectionTaskCreated(ParcelDetectionTask task) {
    super(task);
  }

  @Override
  public Duration maxConsumerDuration() {
    return Duration.ofMinutes(3);
  }

  @Override
  public Duration maxConsumerBackoffBetweenRetries() {
    return Duration.ofMinutes(1);
  }
}
