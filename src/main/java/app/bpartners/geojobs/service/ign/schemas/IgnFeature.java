package app.bpartners.geojobs.service.ign.schemas;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.Map;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class IgnFeature {
  public Map<String, Object> properties;
  public Object geometry;
}
