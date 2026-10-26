package app.bpartners.geojobs.service.threed.exception;

import lombok.Getter;
import org.springframework.http.HttpStatusCode;

@Getter
public class ThreedApiException extends RuntimeException {
  private final HttpStatusCode statusCode;
  private final String apiError;

  /**
   * A short error code identifying the failure, e.g. "NO_LIDAR_DATA_FOUND". Either the {@code type}
   * of an RFC 7807 Problem returned by threed for a genuine HTTP error, or synthesized locally when
   * threed reports the same situation inline as a 200 response with {@code status=NO_LIDAR}.
   */
  private final String apiErrorType;

  public ThreedApiException(
      HttpStatusCode statusCode, String apiErrorType, String apiError, String message) {
    super(message);
    this.statusCode = statusCode;
    this.apiErrorType = apiErrorType;
    this.apiError = apiError;
  }

  public ThreedApiException(String message, Throwable cause) {
    super(message, cause);
    this.statusCode = null;
    this.apiErrorType = null;
    this.apiError = null;
  }

  public boolean isNoLidarDataFound() {
    return "NO_LIDAR_DATA_FOUND".equals(apiErrorType);
  }
}
