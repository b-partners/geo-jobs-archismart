package app.bpartners.geojobs.repository.model.annotation;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.*;

@Entity
@Table(name = "annotation_delivery_configuration")
@Builder
@AllArgsConstructor
@NoArgsConstructor
@ToString(callSuper = true)
@Data
@EqualsAndHashCode(callSuper = false)
public class AnnotationDeliveryConfiguration {
  @Id private String id;
  private Double minimumConfidenceForDelivery;
  private Instant creationDatetime;
}
