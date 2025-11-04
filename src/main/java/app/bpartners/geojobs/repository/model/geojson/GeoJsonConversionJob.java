package app.bpartners.geojobs.repository.model.geojson;

import static jakarta.persistence.EnumType.STRING;
import static org.hibernate.type.SqlTypes.NAMED_ENUM;

import app.bpartners.geojobs.job.model.Job;
import app.bpartners.geojobs.job.model.JobType;
import app.bpartners.geojobs.repository.model.detection.ZoneDetectionJob;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.util.ArrayList;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.JdbcTypeCode;

@Entity
@Table(name = "geo_json_conversion_job")
@SuperBuilder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
@ToString(callSuper = true)
@Data
@EqualsAndHashCode(callSuper = false)
public class GeoJsonConversionJob extends Job {
  private String zoneDetectionJobId;
  private String fileKey;

  @Enumerated(STRING)
  @JdbcTypeCode(NAMED_ENUM)
  private ZoneDetectionJob.DetectionType zoneDetectionJobType;

  @Override
  protected JobType getType() {
    return null;
  }

  @Override
  public Job semanticClone() {
    return this.toBuilder().statusHistory(new ArrayList<>(getStatusHistory())).build();
  }
}
