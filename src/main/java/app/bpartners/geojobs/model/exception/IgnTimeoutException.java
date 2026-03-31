package app.bpartners.geojobs.model.exception;

import static app.bpartners.geojobs.model.exception.ApiException.ExceptionType.SERVER_EXCEPTION;

public class IgnTimeoutException extends ApiException {
  public IgnTimeoutException(String message) {
    super(SERVER_EXCEPTION, message);
  }
}
