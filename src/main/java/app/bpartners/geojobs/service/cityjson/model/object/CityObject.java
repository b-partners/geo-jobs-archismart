package app.bpartners.geojobs.service.cityjson.model.object;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class CityObject {
  private String type; // "Building", "BuildingPart", ...
  private Map<String, Object> attributes;
  private List<String> children;
  private List<String> parents;
  private double[] geographicalExtent;
  private List<Geometry> geometry;

  // getters / setters
  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public Map<String, Object> getAttributes() {
    return attributes;
  }

  public void setAttributes(Map<String, Object> attributes) {
    this.attributes = attributes;
  }

  public List<String> getChildren() {
    return children;
  }

  public void setChildren(List<String> children) {
    this.children = children;
  }

  public List<String> getParents() {
    return parents;
  }

  public void setParents(List<String> parents) {
    this.parents = parents;
  }

  public double[] getGeographicalExtent() {
    return geographicalExtent;
  }

  public void setGeographicalExtent(double[] geographicalExtent) {
    this.geographicalExtent = geographicalExtent;
  }

  public List<Geometry> getGeometry() {
    return geometry;
  }

  public void setGeometry(List<Geometry> geometry) {
    this.geometry = geometry;
  }
}
