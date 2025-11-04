package app.bpartners.geojobs.repository.model;

import app.bpartners.geojobs.repository.model.tiling.ZoneTilingJob;
import lombok.*;

@Data
@Builder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
@ToString
@EqualsAndHashCode
public class DuplicatedTilingJob {
  private ZoneTilingJob originalJob;
  private ZoneTilingJob duplicatedJob;
}
