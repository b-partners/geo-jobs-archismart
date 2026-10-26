package app.bpartners.geojobs.utils.it;

/**
 * One geolocated address to submit to the API, whatever the endpoint under test (2D detection, 3D
 * request…), as read from a CSV holding an {@code address} and a {@code coordinates} column (see
 * {@link AddressPointCsvReader}).
 *
 * @param address human-readable address, as submitted at request creation.
 * @param latitude latitude of the point, first of the {@code coordinates} pair.
 * @param longitude longitude of the point, second of the {@code coordinates} pair.
 */
public record AddressPoint(String address, double latitude, double longitude) {
  public AddressPoint {
    if (address == null || address.isBlank()) {
      throw new IllegalArgumentException("address is required");
    }
  }

  /**
   * Coordinates as {@code lat;lon}, the order used by the CSV — unlike the GeoJSON geometries sent
   * to the API, which store them as {@code [lon, lat]}.
   */
  public String coordinates() {
    return latitude + ";" + longitude;
  }
}
