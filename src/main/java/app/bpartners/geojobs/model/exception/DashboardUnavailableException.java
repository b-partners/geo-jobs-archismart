package app.bpartners.geojobs.model.exception;

public class DashboardUnavailableException extends ApiException {
  public DashboardUnavailableException(Exception source) {
    super(ExceptionType.SERVER_EXCEPTION, source);
  }
}
