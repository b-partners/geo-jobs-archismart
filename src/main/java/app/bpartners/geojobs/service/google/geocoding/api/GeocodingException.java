package app.bpartners.geojobs.service.google.geocoding.api.exception;

public class GeocodingException extends RuntimeException {
  public GeocodingException(String message) {
    super(message);
  }

  public GeocodingException(String message, Throwable cause) {
    super(message, cause);
  }
}
