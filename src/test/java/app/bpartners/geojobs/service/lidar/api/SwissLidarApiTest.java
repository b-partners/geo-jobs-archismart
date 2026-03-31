package app.bpartners.geojobs.service.lidar.api;

import static app.bpartners.geojobs.service.model.SwissBoundaryCheckerTest.switzerland_coords;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestTemplate;

class SwissLidarApiTest {
  RestTemplate restTemplate = new RestTemplate();
  SwissLidarApi swissLidarApi = new SwissLidarApi(restTemplate);

  @Test
  void retrieve_swiss_lidars_urls() {
    var res = swissLidarApi.apply(switzerland_coords().getEnvelopeInternal());

    assertTrue(
        res.contains(
            "https://data.geo.admin.ch/ch.swisstopo.swisssurface3d/swisssurface3d_2025_2499-1115/swisssurface3d_2025_2499-1115_2056_5728.copc.laz"));
  }
}
