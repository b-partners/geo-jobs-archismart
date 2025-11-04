package app.bpartners.geojobs.repository.model.detection;

import static app.bpartners.geojobs.repository.model.GeoJobType.PARCEL_DETECTION;
import static org.hibernate.type.SqlTypes.JSON;

import app.bpartners.geojobs.job.model.Job;
import app.bpartners.geojobs.job.model.JobType;
import app.bpartners.geojobs.repository.model.Feature;
import jakarta.persistence.*;
import java.util.ArrayList;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.JdbcTypeCode;

@Entity
@Getter
@SuperBuilder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class ParcelDetectionJob extends Job {
  private String address;

  @JdbcTypeCode(JSON)
  private Feature point;

  @Override
  protected JobType getType() {
    return PARCEL_DETECTION;
  }

  @Override
  public Job semanticClone() {
    return this.toBuilder().statusHistory(new ArrayList<>(getStatusHistory())).build();
  }
}
