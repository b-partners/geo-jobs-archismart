package app.bpartners.geojobs.service.cityjson.model.object;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.LinkedHashMap;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class CityJSONFeature {
  private String type = "CityJSONFeature";
  private String id;
  private Map<String, CityObject> CityObjects = new LinkedHashMap<>();
  private long[][] vertices = new long[0][];

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  @JsonProperty("CityObjects")
  public Map<String, CityObject> getCityObjects() {
    return CityObjects;
  }

  public void setCityObjects(Map<String, CityObject> cityObjects) {
    this.CityObjects = cityObjects;
  }

  public long[][] getVertices() {
    return vertices;
  }

  public void setVertices(long[][] vertices) {
    this.vertices = vertices;
  }
}
