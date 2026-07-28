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
public class CityJsonProcessorResponse {
  @JsonProperty("id")
  private String id;

  @JsonProperty("fileUrl")
  private String fileUrl;

  @JsonProperty("status")
  private ProcessingStatus status;

  @JsonProperty("delimitationType")
  private DelimitationType delimitationType;
}
