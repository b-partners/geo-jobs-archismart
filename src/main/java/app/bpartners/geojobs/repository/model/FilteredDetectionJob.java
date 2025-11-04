package app.bpartners.geojobs.repository.model;

import app.bpartners.geojobs.repository.model.detection.ZoneDetectionJob;
import lombok.*;

@Data
@ToString
@EqualsAndHashCode(callSuper = true)
public class FilteredDetectionJob extends FilteredJob<ZoneDetectionJob> {
  public FilteredDetectionJob(
      String jobId, ZoneDetectionJob succeededJob, ZoneDetectionJob notSucceededJob) {
    super(jobId, succeededJob, notSucceededJob);
  }
}
