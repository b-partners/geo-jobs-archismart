package app.bpartners.geojobs.service.lidar.api;

import static app.bpartners.geojobs.model.geometry.GeometryFactory.geometryFactory;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import app.bpartners.geojobs.conf.FacadeIT;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.springframework.beans.factory.annotation.Autowired;

@Slf4j
@Disabled("TODO: hits the real geodata service (which itself hits IGN/STAC), run manually only")
class LidarApiFacadeIT extends FacadeIT {
  @Autowired LidarApiFacade subject;

  // Area confirmed (via direct WFS query) to be covered by an actual IGN LidarHD tile
  // (LHD_FXX_0836-6835), unlike an arbitrary area which may fall outside current coverage.
  private static Geometry an_area_covered_by_ign_lidar_hd() {
    var coordinates =
        new Coordinate[] {
          new Coordinate(4.840, 48.588),
          new Coordinate(4.862, 48.588),
          new Coordinate(4.862, 48.604),
          new Coordinate(4.840, 48.604),
          new Coordinate(4.840, 48.588)
        };
    return geometryFactory.createPolygon(coordinates);
  }

  @Test
  void resolves_ign_lidar_hd_urls_via_geodata() {
    var actual = subject.getUniqueLidarFilesUrls(Set.of(an_area_covered_by_ign_lidar_hd()));

    log.info("LidarFilesUrls resolved via real network call: {}", actual.filesUrls().keySet());
    assertFalse(actual.filesUrls().isEmpty());
    assertTrue(
        actual.filesUrls().keySet().stream()
            .allMatch(url -> url.startsWith("https://data.geopf.fr/")));
  }
}
