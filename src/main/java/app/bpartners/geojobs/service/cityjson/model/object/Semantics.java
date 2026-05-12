package app.bpartners.geojobs.service.cityjson.model.object;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class Semantics {
  private List<Map<String, Object>> surfaces;
  private Object values; // tableau imbriqué d'entiers (profondeur variable)

  public List<Map<String, Object>> getSurfaces() {
    return surfaces;
  }

  public void setSurfaces(List<Map<String, Object>> surfaces) {
    this.surfaces = surfaces;
  }

  public Object getValues() {
    return values;
  }

  public void setValues(Object values) {
    this.values = values;
  }
}
