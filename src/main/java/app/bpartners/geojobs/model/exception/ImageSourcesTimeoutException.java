package app.bpartners.geojobs.model.exception;

public class ImageSourcesTimeoutException extends GatewayTimeoutException {

  public ImageSourcesTimeoutException(String message) {
    super(message);
  }
}
