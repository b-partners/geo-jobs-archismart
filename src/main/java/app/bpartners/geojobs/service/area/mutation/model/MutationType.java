package app.bpartners.geojobs.service.area.mutation.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.io.Serializable;

public enum MutationType implements Serializable {
  @JsonProperty("background")
  BACKGROUND,
  @JsonProperty("improvement")
  IMPROVEMENT,
  @JsonProperty("deterioration")
  DETERIORATION,
  @JsonProperty("RAS")
  RAS,
  @JsonProperty("none")
  NONE,
  @JsonProperty("unknown")
  UNKNOWN
}
