package app.bpartners.geojobs.service.threed.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateLrgFromFeatureFileUrl {
  @JsonProperty("featureFileUrl")
  private String featureFileUrl;

  @JsonProperty("delimitationType")
  private DelimitationType delimitationType;
}
