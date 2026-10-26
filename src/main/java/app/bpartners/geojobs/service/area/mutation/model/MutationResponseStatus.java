package app.bpartners.geojobs.service.area.mutation.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.io.Serializable;

public enum MutationResponseStatus implements Serializable {
  @JsonProperty("success")
  SUCCESS
}
