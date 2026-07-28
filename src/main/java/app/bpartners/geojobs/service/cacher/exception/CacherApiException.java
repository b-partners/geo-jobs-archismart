package app.bpartners.geojobs.service.cacher.exception;

import lombok.Getter;
import org.springframework.http.HttpStatusCode;

@Getter
public class CacherApiException extends RuntimeException {
  private final HttpStatusCode statusCode;
  private final String apiError;

  public CacherApiException(HttpStatusCode statusCode, String apiError, String message) {
    super(message);
    this.statusCode = statusCode;
    this.apiError = apiError;
  }

  public CacherApiException(String message, Throwable cause) {
    super(message, cause);
    this.statusCode = null;
    this.apiError = null;
  }
}
