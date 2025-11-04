package app.bpartners.geojobs.endpoint.event.model;

import app.bpartners.geojobs.repository.model.geojson.GeoJsonConversionTask;
import lombok.*;

@Getter
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@ToString
public class GeoJsonConversionTaskCreated extends TaskCreated<GeoJsonConversionTask> {
  public GeoJsonConversionTaskCreated(GeoJsonConversionTask task) {
    super(task);
  }
}
