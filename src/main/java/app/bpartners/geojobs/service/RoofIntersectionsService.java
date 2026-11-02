package app.bpartners.geojobs.service;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.UNPROCESSABLE_ENTITY;

import app.bpartners.geojobs.endpoint.rest.model.CreateRoofIntersections;
import app.bpartners.geojobs.endpoint.rest.model.RoofIntersections;
import app.bpartners.geojobs.model.exception.BadRequestException;
import app.bpartners.geojobs.service.threed.ThreedApiClient;
import app.bpartners.geojobs.service.threed.exception.ThreedApiException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RoofIntersectionsService {
  private final ThreedApiClient threedApiClient;

  /**
   * threed refuses a zone it can not read the roof under (400, or 422 e.g. when no lidar covers
   * it): this is the client's zone, so it is reported back as a bad request, not as a server error.
   */
  public RoofIntersections compute(String id, CreateRoofIntersections createRoofIntersections) {
    try {
      return threedApiClient.computeIntersections(id, createRoofIntersections);
    } catch (ThreedApiException e) {
      if (e.getStatusCode() != null
          && (e.getStatusCode().isSameCodeAs(BAD_REQUEST)
              || e.getStatusCode().isSameCodeAs(UNPROCESSABLE_ENTITY))) {
        throw new BadRequestException(e.getApiError() != null ? e.getApiError() : e.getMessage());
      }
      throw e;
    }
  }
}
