package app.bpartners.geojobs.endpoint.rest.controller.mapper.cityjson;

import app.bpartners.geojobs.endpoint.rest.model.CityJSONRequestStatus;
import app.bpartners.geojobs.endpoint.rest.model.Status;
import java.time.Instant;

public class CityJSONRequestStatusMapper {
  private CityJSONRequestStatusMapper() {}

  public static CityJSONRequestStatus toRest(
      app.bpartners.geojobs.repository.model.cityjson.CityJSONRequestStatus status) {
    return switch (status) {
      case null -> null;
      case FINISHED -> CityJSONRequestStatus.FINISHED;
      case UNAVAILABLE -> CityJSONRequestStatus.UNAVAILABLE;
      case FAILED -> CityJSONRequestStatus.FAILED;
      case PROCESSING -> CityJSONRequestStatus.PROCESSING;
    };
  }

  // TODO: bad static method
  public static Status toGenericStatusRest(
      app.bpartners.geojobs.repository.model.cityjson.CityJSONRequestStatus status) {
    switch (status) {
      case FINISHED -> {
        return new Status()
            .progression(Status.ProgressionEnum.FINISHED)
            .health(Status.HealthEnum.SUCCEEDED)
            .creationDatetime(Instant.now());
      }
      case UNAVAILABLE, FAILED -> {
        return new Status()
            .progression(Status.ProgressionEnum.FINISHED)
            .health(Status.HealthEnum.FAILED)
            .creationDatetime(Instant.now());
      }
      case PROCESSING -> {
        return new Status()
            .progression(Status.ProgressionEnum.PROCESSING)
            .health(Status.HealthEnum.UNKNOWN)
            .creationDatetime(Instant.now());
      }
      default -> {
        return new Status()
            .progression(Status.ProgressionEnum.PENDING)
            .health(Status.HealthEnum.UNKNOWN)
            .creationDatetime(Instant.now());
      }
    }
  }
}
