package app.bpartners.geojobs.repository.model.annotation;

import static app.bpartners.geojobs.repository.model.GeoJobType.ANNOTATION_DELIVERY;
import static org.hibernate.type.SqlTypes.JSON;

import app.bpartners.gen.annotator.endpoint.rest.model.CreateAnnotatedTask;
import app.bpartners.geojobs.job.model.JobType;
import app.bpartners.geojobs.job.model.Task;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.util.ArrayList;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.JdbcTypeCode;

@Entity
@Table(name = "annotation_delivery_task")
@SuperBuilder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
@ToString
@Data
@EqualsAndHashCode(callSuper = false)
public class AnnotationDeliveryTask extends Task {
  private String annotationTaskId;
  private String annotationJobId;

  @JdbcTypeCode(JSON)
  private CreateAnnotatedTask createAnnotatedTask;

  @Override
  public JobType getJobType() {
    return ANNOTATION_DELIVERY;
  }

  @Override
  public Task semanticClone() {
    return this.toBuilder().statusHistory(new ArrayList<>(getStatusHistory())).build();
  }
}
