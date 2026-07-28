package app.bpartners.geojobs.service.ciytjsonprocessor.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateCityJsonFromFeatureFileUrl {
  @JsonProperty("featureFileUrl")
  private String featureFileUrl;

  @JsonProperty("delimitationType")
  private DelimitationType delimitationType;
}
