package app.bpartners.geojobs.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import app.bpartners.geojobs.endpoint.rest.model.CreateRoofIntersections;
import app.bpartners.geojobs.endpoint.rest.model.RoofIntersections;
import app.bpartners.geojobs.model.exception.BadRequestException;
import app.bpartners.geojobs.service.threed.ThreedApiClient;
import app.bpartners.geojobs.service.threed.exception.ThreedApiException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

class RoofIntersectionsServiceTest {
  private final ThreedApiClient threedApiClientMock = mock();
  private final RoofIntersectionsService subject =
      new RoofIntersectionsService(threedApiClientMock);

  @Test
  void should_return_threed_intersections() {
    var expected = new RoofIntersections().id("zone-1");
    when(threedApiClientMock.computeIntersections(eq("zone-1"), any())).thenReturn(expected);

    assertSame(expected, subject.compute("zone-1", new CreateRoofIntersections()));
  }

  @Test
  void should_report_a_refused_zone_as_bad_request() {
    when(threedApiClientMock.computeIntersections(any(), any()))
        .thenThrow(
            new ThreedApiException(
                HttpStatus.UNPROCESSABLE_ENTITY,
                "NO_LIDAR_DATA_FOUND",
                "No lidar under the zone",
                "Threed API error"));

    var actual =
        assertThrows(
            BadRequestException.class,
            () -> subject.compute("zone-1", new CreateRoofIntersections()));

    assertEquals("No lidar under the zone", actual.getMessage());
  }

  @Test
  void should_keep_threed_server_errors() {
    var error =
        new ThreedApiException(HttpStatus.INTERNAL_SERVER_ERROR, null, "boom", "Threed API error");
    when(threedApiClientMock.computeIntersections(any(), any())).thenThrow(error);

    assertSame(
        error,
        assertThrows(
            ThreedApiException.class,
            () -> subject.compute("zone-1", new CreateRoofIntersections())));
  }
}
