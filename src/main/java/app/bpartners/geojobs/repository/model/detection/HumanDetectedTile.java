package app.bpartners.geojobs.repository.model.detection;

import static jakarta.persistence.CascadeType.ALL;
import static jakarta.persistence.FetchType.EAGER;
import static org.hibernate.type.SqlTypes.JSON;

import app.bpartners.geojobs.repository.model.tiling.Tile;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.io.Serializable;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.JdbcTypeCode;

@Entity
@Table(name = "human_detected_tile")
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Getter
@Setter
@EqualsAndHashCode(callSuper = false)
@ToString
public class HumanDetectedTile implements Serializable {
  @Id private String id;
  private String jobId;
  private String annotationJobId;
  private String machineDetectedTileId;
  private int imageSize;

  @JdbcTypeCode(JSON)
  private Tile tile;

  @OneToMany(cascade = ALL, mappedBy = "detectedTileId", fetch = EAGER)
  private List<DetectedObject> detectedObjects;
}
