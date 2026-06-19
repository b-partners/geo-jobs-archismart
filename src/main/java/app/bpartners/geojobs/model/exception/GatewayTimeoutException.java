package app.bpartners.geojobs.model.exception;

import static app.bpartners.geojobs.model.exception.ApiException.ExceptionType.SERVER_EXCEPTION;

public class GatewayTimeoutException extends ApiException {
  public GatewayTimeoutException(String message) {
    super(SERVER_EXCEPTION, message);
  }
}
