package app.bpartners.geojobs.repository.model.detection;

import static app.bpartners.geojobs.repository.model.GeoJobType.DETECTION;
import static jakarta.persistence.CascadeType.ALL;
import static jakarta.persistence.EnumType.STRING;
import static org.hibernate.type.SqlTypes.NAMED_ENUM;

import app.bpartners.geojobs.job.model.Job;
import app.bpartners.geojobs.job.model.JobType;
import app.bpartners.geojobs.repository.model.tiling.ZoneTilingJob;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.OneToOne;
import java.util.ArrayList;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.JdbcTypeCode;

@Entity
@Getter
@SuperBuilder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
@ToString(exclude = "zoneTilingJob", callSuper = true)
@Setter
public class ZoneDetectionJob extends Job {
  @OneToOne(cascade = ALL)
  private ZoneTilingJob zoneTilingJob;

  @Enumerated(STRING)
  @JdbcTypeCode(NAMED_ENUM)
  private DetectionType detectionType;

  @Override
  protected JobType getType() {
    return DETECTION;
  }

  @Override
  public Job semanticClone() {
    return this.toBuilder().statusHistory(new ArrayList<>(getStatusHistory())).build();
  }

  public enum DetectionType {
    MACHINE,
    HUMAN
  }

  public ZoneDetectionJob duplicate(String jobId) {
    return ZoneDetectionJob.builder()
        .id(jobId)
        .zoneName(this.zoneName)
        .zoneTilingJob(this.zoneTilingJob)
        .emailReceiver(this.emailReceiver)
        .detectionType(this.getDetectionType())
        .statusHistory(this.getStatusHistory())
        .submissionInstant(this.getSubmissionInstant())
        .build();
  }
}
