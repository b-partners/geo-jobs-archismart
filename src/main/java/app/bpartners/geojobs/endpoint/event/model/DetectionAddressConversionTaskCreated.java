package app.bpartners.geojobs.endpoint.event.model;

import app.bpartners.geojobs.repository.model.DetectionAddressConversionTask;
import lombok.*;

@Getter
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@ToString
public class DetectionAddressConversionTaskCreated
    extends TaskCreated<DetectionAddressConversionTask> {

  public DetectionAddressConversionTaskCreated(
      DetectionAddressConversionTask task, String e2ApiKey) {
    super(task);
    this.e2ApiKey = e2ApiKey;
  }

  private String e2ApiKey;
}
