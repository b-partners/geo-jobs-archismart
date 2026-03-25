package app.bpartners.geojobs.model.exception;

import static app.bpartners.geojobs.model.exception.ApiException.ExceptionType.SERVER_EXCEPTION;

public class ImageSourcesTimeoutException extends ApiException {

  public ImageSourcesTimeoutException(String message) {
    super(SERVER_EXCEPTION, message);
  }
}
