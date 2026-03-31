package app.bpartners.geojobs.model.lidar.planes.topology.model;

import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.locationtech.jts.geom.LineString;

@Getter
@Builder(toBuilder = true)
@RequiredArgsConstructor
public class Rupture {
  private final LineString line;
}
