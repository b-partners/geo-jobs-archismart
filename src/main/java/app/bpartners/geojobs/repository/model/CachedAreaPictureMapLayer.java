package app.bpartners.geojobs.repository.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import java.io.Serializable;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
@Entity(name = "cached_area_picture_map_layer")
public class CachedAreaPictureMapLayer implements Serializable {
  @Id private String id;

  @Column private String layerName;

  @Column private Integer precisionLevelInCm;

  @Column private Integer layerYear;

  @Column private String layerSource;

  @Column private Instant cachedAt;
}
