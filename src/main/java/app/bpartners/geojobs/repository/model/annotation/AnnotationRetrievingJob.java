package app.bpartners.geojobs.repository.model.annotation;

import app.bpartners.geojobs.job.model.Job;
import app.bpartners.geojobs.job.model.JobType;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.util.ArrayList;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "annotation_retrieving_job")
@SuperBuilder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
@ToString(callSuper = true)
@Data
@EqualsAndHashCode(callSuper = false)
public class AnnotationRetrievingJob extends Job {
  private String annotationJobId;
  private String detectionJobId;

  @Override
  protected JobType getType() {
    return null;
  }

  @Override
  public Job semanticClone() {
    return this.toBuilder().statusHistory(new ArrayList<>(getStatusHistory())).build();
  }
}
