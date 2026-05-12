package app.bpartners.geojobs.service.cityjson.model.object;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class Geometry {
  private String type; // "MultiSurface", "Solid", ...
  private String lod; // "0", "1.2", "2.2", ...
  private Object boundaries; // structure imbriquée d'entiers
  private Semantics semantics;

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public String getLod() {
    return lod;
  }

  public void setLod(String lod) {
    this.lod = lod;
  }

  public Object getBoundaries() {
    return boundaries;
  }

  public void setBoundaries(Object boundaries) {
    this.boundaries = boundaries;
  }

  public Semantics getSemantics() {
    return semantics;
  }

  public void setSemantics(Semantics semantics) {
    this.semantics = semantics;
  }
}
