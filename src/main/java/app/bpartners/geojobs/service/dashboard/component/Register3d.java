package app.bpartners.geojobs.service.dashboard.component;

public record Register3d(String address, String externalReference) {
  public Register3d(String address) {
    this(address, null);
  }
}
