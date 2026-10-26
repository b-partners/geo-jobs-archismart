package app.bpartners.geojobs.service.threed.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Lrg {
  @JsonProperty("id")
  private String id;

  @JsonProperty("fileUrl")
  private String fileUrl;

  @JsonProperty("status")
  private GenerationStatus status;

  @JsonProperty("delimitationType")
  private DelimitationType delimitationType;

  @JsonProperty("message")
  private String message;

  /** Known, non-fatal reasons the generation is degraded, e.g. "GROUND_ONLY_PANELS". */
  @JsonProperty("warnings")
  private List<String> warnings;
}
