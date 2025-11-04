package app.bpartners.geojobs.repository.model.geojson;

import static app.bpartners.geojobs.repository.model.GeoJobType.GEO_JSON_CONVERSION;
import static jakarta.persistence.EnumType.STRING;
import static org.hibernate.type.SqlTypes.NAMED_ENUM;

import app.bpartners.geojobs.job.model.JobType;
import app.bpartners.geojobs.job.model.Task;
import app.bpartners.geojobs.repository.model.detection.DetectableType;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.util.ArrayList;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.JdbcTypeCode;

@Entity
@Table(name = "\"geo_json_conversion_task\"")
@SuperBuilder(toBuilder = true)
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@EqualsAndHashCode(callSuper = false)
public class GeoJsonConversionTask extends Task {
  private Integer page;
  private String fileKey;

  @Enumerated(STRING)
  @JdbcTypeCode(NAMED_ENUM)
  private DetectableType detectableType;

  @Override
  public JobType getJobType() {
    return GEO_JSON_CONVERSION;
  }

  @Override
  public Task semanticClone() {
    return this.toBuilder().statusHistory(new ArrayList<>(getStatusHistory())).build();
  }
}
