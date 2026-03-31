package app.bpartners.geojobs.model.lidar.planes.topology.model;

import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@Builder(toBuilder = true)
@RequiredArgsConstructor
public class RoofTopology {
  private final boolean[][] adjacency;
  private final Rupture[][] ruptures;
  private final RoofRelationType[][] relations;
}
