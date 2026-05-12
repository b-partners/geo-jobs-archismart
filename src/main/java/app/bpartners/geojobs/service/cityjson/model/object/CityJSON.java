package app.bpartners.geojobs.service.cityjson.model.object;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.LinkedHashMap;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class CityJSON {
  private String type = "CityJSON";
  private String version = "2.0";
  private Map<String, CityObject> CityObjects = new LinkedHashMap<>();
  private Metadata metadata;
  private Transform transform;
  private long[][] vertices = new long[0][];

  // getters / setters
  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public String getVersion() {
    return version;
  }

  public void setVersion(String version) {
    this.version = version;
  }

  @JsonProperty("CityObjects")
  public Map<String, CityObject> getCityObjects() {
    return CityObjects;
  }

  public void setCityObjects(Map<String, CityObject> cityObjects) {
    this.CityObjects = cityObjects;
  }

  public Metadata getMetadata() {
    return metadata;
  }

  public void setMetadata(Metadata metadata) {
    this.metadata = metadata;
  }

  public Transform getTransform() {
    return transform;
  }

  public void setTransform(Transform transform) {
    this.transform = transform;
  }

  public long[][] getVertices() {
    return vertices;
  }

  public void setVertices(long[][] vertices) {
    this.vertices = vertices;
  }
}
