package app.bpartners.geojobs.service.cityjson.model.object;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class Metadata {
  private double[] geographicalExtent;
  private String identifier;
  private String referenceDate;
  private String referenceSystem;

  public double[] getGeographicalExtent() {
    return geographicalExtent;
  }

  public void setGeographicalExtent(double[] g) {
    this.geographicalExtent = g;
  }

  public String getIdentifier() {
    return identifier;
  }

  public void setIdentifier(String identifier) {
    this.identifier = identifier;
  }

  public String getReferenceDate() {
    return referenceDate;
  }

  public void setReferenceDate(String referenceDate) {
    this.referenceDate = referenceDate;
  }

  public String getReferenceSystem() {
    return referenceSystem;
  }

  public void setReferenceSystem(String referenceSystem) {
    this.referenceSystem = referenceSystem;
  }
}
