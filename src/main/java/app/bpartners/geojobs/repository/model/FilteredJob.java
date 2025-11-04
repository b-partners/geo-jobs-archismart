package app.bpartners.geojobs.repository.model;

import app.bpartners.geojobs.job.model.Job;
import lombok.*;

@Data
@Builder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
@ToString
@EqualsAndHashCode
public class FilteredJob<J extends Job> {
  protected String initialJobId;
  protected J succeededJob;
  protected J notSucceededJob;
}
