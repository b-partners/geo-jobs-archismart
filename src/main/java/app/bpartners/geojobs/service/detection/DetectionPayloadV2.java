package app.bpartners.geojobs.service.detection;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
@ToString
@EqualsAndHashCode
public class DetectionPayloadV2 {
  @JsonProperty("filename")
  private String fileName;

  @JsonProperty("image_base64")
  private String base64ImgData;

  @JsonProperty("mask_base64")
  @JsonInclude(JsonInclude.Include.NON_NULL)
  private String base64MaskData;

  @JsonProperty("vegetation")
  private Boolean vegetation;
}
