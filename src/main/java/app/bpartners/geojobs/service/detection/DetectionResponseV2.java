package app.bpartners.geojobs.service.detection;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@JsonIgnoreProperties(ignoreUnknown = true)
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@ToString
@Builder
public class DetectionResponseV2 {
  public static final String REGION_LABEL_PROPERTY = "label";
  public static final String REGION_CONFIDENCE_PROPERTY = "confidence";

  @Builder.Default private Map<String, ImageData> images = new HashMap<>();

  @JsonAnyGetter
  public Map<String, ImageData> getImages() {
    return images;
  }

  @JsonAnySetter
  public void setImage(String filename, ImageData imageData) {
    this.images.put(filename, imageData);
  }

  @ToString
  @Getter
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class ImageData {
    @JsonProperty("fileref")
    private String fileref;

    @JsonProperty("size")
    private String size;

    @JsonProperty("filename")
    private String filename;

    @JsonProperty("base64_img_data")
    private String base64ImgData;

    @JsonProperty("file_attributes")
    private Map<String, Object> fileAttributes;

    @JsonProperty("regions")
    @Setter
    private Map<String, Region> regions;

    @ToString
    @Getter
    @AllArgsConstructor
    @Builder
    @NoArgsConstructor
    public static class Region {
      @JsonProperty("shape_attributes")
      private ShapeAttributes shapeAttributes;

      @JsonProperty("region_attributes")
      private Map<String, String> regionAttributes;
    }

    @ToString
    @Getter
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ShapeAttributes {
      @JsonProperty("name")
      private String name;

      @JsonProperty("all_points_x")
      private List<BigDecimal> allPointsX;

      @JsonProperty("all_points_y")
      private List<BigDecimal> allPointsY;
    }
  }
}
