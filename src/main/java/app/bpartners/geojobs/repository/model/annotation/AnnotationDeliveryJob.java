package app.bpartners.geojobs.repository.model.annotation;

import static org.hibernate.type.SqlTypes.JSON;

import app.bpartners.gen.annotator.endpoint.rest.model.Label;
import app.bpartners.geojobs.job.model.Job;
import app.bpartners.geojobs.job.model.JobType;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.JdbcTypeCode;

@Entity
@Table(name = "annotation_delivery_job")
@SuperBuilder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
@ToString(callSuper = true)
@Data
@EqualsAndHashCode(callSuper = false)
public class AnnotationDeliveryJob extends Job {
  private String annotationJobId;
  private String annotationJobName;
  private String detectionJobId;

  // TODO: must be persist and NOT using rest model
  @JdbcTypeCode(JSON)
  private List<Label> labels;

  @Override
  protected JobType getType() {
    return null;
  }

  @Override
  public Job semanticClone() {
    return this.toBuilder().statusHistory(new ArrayList<>(getStatusHistory())).build();
  }
}
