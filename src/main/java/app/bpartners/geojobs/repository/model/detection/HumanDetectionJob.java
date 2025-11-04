package app.bpartners.geojobs.repository.model.detection;

import static jakarta.persistence.CascadeType.ALL;
import static jakarta.persistence.FetchType.EAGER;
import static org.hibernate.type.SqlTypes.JSON;

import jakarta.persistence.*;
import java.util.List;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.JdbcTypeCode;

@Entity
@Data
@SuperBuilder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class HumanDetectionJob {
  @Id private String id;
  private String annotationJobId;

  @JoinColumn(referencedColumnName = "id")
  private String zoneDetectionJobId;

  @OneToMany(fetch = EAGER, orphanRemoval = true, cascade = ALL)
  @JoinColumn(name = "human_detection_job_id")
  private List<MachineDetectedTile> machineDetectedTiles;

  @JdbcTypeCode(JSON)
  private List<DetectableObjectConfiguration> detectableObjectConfigurations;
}
