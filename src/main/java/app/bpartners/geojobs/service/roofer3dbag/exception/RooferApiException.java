package app.bpartners.geojobs.service.roofer3dbag.exception;

import lombok.Getter;
import org.springframework.http.HttpStatusCode;

/** Exception levée lorsque l'API roofer renvoie une erreur ou est injoignable. */
@Getter
public class RooferApiException extends RuntimeException {

  private final HttpStatusCode statusCode;
  private final String apiError;

  public RooferApiException(HttpStatusCode statusCode, String apiError, String message) {
    super(message);
    this.statusCode = statusCode;
    this.apiError = apiError;
  }

  public RooferApiException(String message, Throwable cause) {
    super(message, cause);
    this.statusCode = null;
    this.apiError = null;
  }
}
