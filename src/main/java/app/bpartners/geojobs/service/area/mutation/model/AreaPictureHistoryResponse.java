package app.bpartners.geojobs.service.area.mutation.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.io.Serializable;
import java.util.List;

/** Mirrors bpartners-geodata's {@code POST /areaPictureHistory} response. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record AreaPictureHistoryResponse(@JsonProperty("images") List<DatedImage> images)
    implements Serializable {

  @JsonIgnoreProperties(ignoreUnknown = true)
  public record DatedImage(
      @JsonProperty("year") int year,
      @JsonProperty("imagePresignedUrl") PresignedUrl imagePresignedUrl)
      implements Serializable {}

  @JsonIgnoreProperties(ignoreUnknown = true)
  public record PresignedUrl(@JsonProperty("value") String value) implements Serializable {}
}
