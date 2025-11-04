package app.bpartners.geojobs.service.detection;

import app.bpartners.geojobs.repository.model.detection.DetectableType;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TileDetectorUrl implements Serializable {
  @JsonProperty("objectType")
  private DetectableType objectType;

  @JsonProperty("url")
  private String url;
}
