package app.bpartners.geojobs.service.lidar.api;

import static app.bpartners.geojobs.model.geometry.GeometryFactory.geometryFactory;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import app.bpartners.geojobs.model.exception.GatewayTimeoutException;
import app.bpartners.geojobs.service.geodata.GeodataHttpClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;

class GeodataLidarApiClientTest {
  GeodataHttpClient httpClientMock = mock();
  GeodataLidarApiClient subject = new GeodataLidarApiClient(new ObjectMapper(), httpClientMock);
  Coordinate[] ring = {
    new Coordinate(4.84, 48.588),
    new Coordinate(4.862, 48.588),
    new Coordinate(4.862, 48.604),
    new Coordinate(4.84, 48.588)
  };

  @Test
  void get_lidar_file_urls_ok() {
    var response =
        new GeodataLidarApiClient.LidarUrlsResponse(
            "FRANCE",
            "EPSG:2154",
            List.of(
                new GeodataLidarApiClient.LidarUrlsResultItem(List.of("a.laz", "b.laz")),
                new GeodataLidarApiClient.LidarUrlsResultItem(null)));
    when(httpClientMock.post(eq("/lidar/urls"), any(String.class), any())).thenReturn(response);

    var actual = subject.getLidarFileUrls(List.of(geometryFactory.createPolygon(ring)));

    assertEquals("FRANCE", actual.region());
    assertEquals("EPSG:2154", actual.crs());
    assertEquals(List.of(Set.of("a.laz", "b.laz"), Set.of()), actual.urlsPerGeometry());
  }

  @Test
  void get_lidar_file_urls_propagates_gateway_timeout() {
    when(httpClientMock.post(eq("/lidar/urls"), any(String.class), any()))
        .thenThrow(new GatewayTimeoutException("down"));

    assertThrows(
        GatewayTimeoutException.class,
        () -> subject.getLidarFileUrls(List.of(geometryFactory.createPolygon(ring))));
  }

  @Test
  void get_lidar_file_urls_without_results_has_no_urls() {
    when(httpClientMock.post(eq("/lidar/urls"), any(String.class), any()))
        .thenReturn(new GeodataLidarApiClient.LidarUrlsResponse("FRANCE", "EPSG:2154", null));

    var actual = subject.getLidarFileUrls(List.of(geometryFactory.createPolygon(ring)));

    assertEquals(List.of(), actual.urlsPerGeometry());
  }
}
