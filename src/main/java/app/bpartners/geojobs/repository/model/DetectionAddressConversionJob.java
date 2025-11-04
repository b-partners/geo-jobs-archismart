package app.bpartners.geojobs.repository.model;

import static app.bpartners.geojobs.repository.model.GeoJobType.DETECTION_ADDRESS_CONVERSION;

import app.bpartners.geojobs.job.model.Job;
import app.bpartners.geojobs.job.model.JobType;
import jakarta.persistence.Entity;
import java.util.ArrayList;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Entity
@Getter
@SuperBuilder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(callSuper = false)
@Setter
public class DetectionAddressConversionJob extends Job {
  private String detectionId;

  @Override
  protected JobType getType() {
    return DETECTION_ADDRESS_CONVERSION;
  }

  @Override
  public Job semanticClone() {
    return this.toBuilder().statusHistory(new ArrayList<>(getStatusHistory())).build();
  }
}
