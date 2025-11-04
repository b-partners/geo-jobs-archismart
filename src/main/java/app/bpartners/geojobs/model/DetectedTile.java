package app.bpartners.geojobs.model;

import app.bpartners.geojobs.repository.model.detection.DetectedObject;
import app.bpartners.geojobs.repository.model.tiling.Tile;
import java.util.List;
import lombok.*;

@AllArgsConstructor
@NoArgsConstructor
@Builder(toBuilder = true)
@Data
@EqualsAndHashCode(callSuper = false)
@ToString
public class DetectedTile {
  private Tile tile;

  private List<DetectedObject> detectedObjects;
}
