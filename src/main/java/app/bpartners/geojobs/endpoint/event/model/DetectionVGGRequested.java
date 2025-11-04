package app.bpartners.geojobs.endpoint.event.model;

import static app.bpartners.geojobs.endpoint.event.EventStack.EVENT_STACK_2;

import app.bpartners.geojobs.endpoint.event.EventStack;
import app.bpartners.geojobs.model.geometry.TiledPixelPolygonSerializable;
import java.time.Duration;
import java.util.List;
import lombok.*;

@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
@Getter
@EqualsAndHashCode(callSuper = false)
@ToString
public class DetectionVGGRequested extends PojaEvent {
  private String detectionId;
  private List<TiledPixelPolygonSerializable> filteredTiledPixelPolygons;

  @Override
  public Duration maxConsumerDuration() {
    return Duration.ofSeconds(120);
  }

  @Override
  public Duration maxConsumerBackoffBetweenRetries() {
    return Duration.ofSeconds(60);
  }

  @Override
  public EventStack getEventStack() {
    return EVENT_STACK_2;
  }
}
