package app.bpartners.geojobs.repository.model.annotation;

import app.bpartners.geojobs.job.model.JobType;
import app.bpartners.geojobs.job.model.Task;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "annotation_retrieving_task")
@SuperBuilder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
@ToString
@Data
@EqualsAndHashCode(callSuper = false)
public class AnnotationRetrievingTask extends Task {
  private String annotationTaskId;
  private String annotationJobId;
  private String humanZoneDetectionJobId;
  private int xTile;
  private int yTile;
  private int zoom;

  @Override
  public JobType getJobType() {
    return null;
  }

  @Override
  public Task semanticClone() {
    return null;
  }
}
