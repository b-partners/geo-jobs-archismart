package app.bpartners.geojobs.model.exception;

public class PaymentRequiredException extends ApiException {
  public PaymentRequiredException(String message) {
    super(ExceptionType.CLIENT_EXCEPTION, message);
  }
}
