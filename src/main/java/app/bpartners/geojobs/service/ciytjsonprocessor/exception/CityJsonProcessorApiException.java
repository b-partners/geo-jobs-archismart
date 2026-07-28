package app.bpartners.geojobs.service.ciytjsonprocessor.exception;

import lombok.Getter;
import org.springframework.http.HttpStatusCode;

@Getter
public class CityJsonProcessorApiException extends RuntimeException {
  private final HttpStatusCode statusCode;
  private final String apiError;

  public CityJsonProcessorApiException(HttpStatusCode statusCode, String apiError, String message) {
    super(message);
    this.statusCode = statusCode;
    this.apiError = apiError;
  }

  public CityJsonProcessorApiException(String message, Throwable cause) {
    super(message, cause);
    this.statusCode = null;
    this.apiError = null;
  }
}
