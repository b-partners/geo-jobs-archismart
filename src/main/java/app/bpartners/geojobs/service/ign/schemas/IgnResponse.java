package app.bpartners.geojobs.service.ign.schemas;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class IgnResponse {
  public List<IgnFeature> features;
}
