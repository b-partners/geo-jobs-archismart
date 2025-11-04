package app.bpartners.geojobs.repository.model;

import app.bpartners.geojobs.repository.model.tiling.ZoneTilingJob;
import lombok.*;

@Data
@ToString
@EqualsAndHashCode(callSuper = true)
public class FilteredTilingJob extends FilteredJob<ZoneTilingJob> {
  public FilteredTilingJob(
      String jobId, ZoneTilingJob succeededJob, ZoneTilingJob notSucceededJob) {
    super(jobId, succeededJob, notSucceededJob);
  }
}
