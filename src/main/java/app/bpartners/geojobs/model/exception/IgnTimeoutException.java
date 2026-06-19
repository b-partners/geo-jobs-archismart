package app.bpartners.geojobs.model.exception;

public class IgnTimeoutException extends GatewayTimeoutException {
  public IgnTimeoutException(String message) {
    super(message);
  }
}
