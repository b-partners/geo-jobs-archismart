package app.bpartners.geojobs.repository.model;

import app.bpartners.geojobs.job.model.TaskStatus;
import lombok.*;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@ToString
@EqualsAndHashCode
@Data
public class ParcelTask {
  private Parcel parcel;
  private TaskStatus status;
}
