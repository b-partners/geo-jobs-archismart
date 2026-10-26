package app.bpartners.geojobs.service.lidar.api;

import static app.bpartners.geojobs.model.geometry.GeometryFactory.geometryFactory;
import static app.bpartners.geojobs.service.GeometrySquareMeterArea.WGS84;
import static app.bpartners.geojobs.service.lidar.api.LidarApiFacade.resolveCrs;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import app.bpartners.geojobs.service.GeometrySquareMeterArea;
import app.bpartners.geojobs.service.geodata.GeodataApiConf;
import app.bpartners.geojobs.service.geodata.GeodataHttpClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.springframework.web.client.RestTemplate;

/**
 * Hits the real, deployed geodata {@code POST /lidar/urls} endpoint directly (bypassing the Spring
 * context, whose test {@code EnvConf} always stubs {@code geodata.api.url}/{@code geodata.api.key}
 * with dummy values), to check the three regions it can resolve: France, Switzerland and Wallonia.
 */
@Slf4j
@Disabled("run locally")
class GeodataLidarApiClientIT {
  private final String GEODATA_API_URL = System.getenv("GEODATA_API_URL");
  private final String GEODATA_API_KEY = System.getenv("GEODATA_API_KEY");
  GeodataLidarApiClient subject =
      new GeodataLidarApiClient(
          new ObjectMapper(),
          new GeodataHttpClient(
              new ObjectMapper(),
              new RestTemplate(),
              new GeodataApiConf(GEODATA_API_URL, GEODATA_API_KEY)));
  private final GeometrySquareMeterArea projector = new GeometrySquareMeterArea();

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
  void resolves_ign_lidar_hd_urls() {
    var actual = subject.getLidarFileUrls(List.of(an_area_covered_by_ign_lidar_hd()));
    var geometriesList = List.of(an_area_covered_by_ign_lidar_hd());
    var targetCrs = resolveCrs(actual.region());

    Map<String, Set<Geometry>> filesUrls = new HashMap<>();
    for (int i = 0; i < geometriesList.size(); i++) {
      var wgs84Geometry = geometriesList.get(i);
      var targetGeometry = projector.project(wgs84Geometry, WGS84, targetCrs);
      var urls =
          i < actual.urlsPerGeometry().size() ? actual.urlsPerGeometry().get(i) : Set.<String>of();

      for (var url : urls) {
        log.info("LAZ File to download: {}", url);
        filesUrls.computeIfAbsent(url, key -> new HashSet<>()).add(targetGeometry);
      }
    }

    log.info(
        "Results from API Facade={}", new LidarApiFacade.LidarFilesResult(filesUrls, targetCrs));
    log.info("region={} crs={} urls={}", actual.region(), actual.crs(), actual.urlsPerGeometry());
    assertEquals("FRANCE", actual.region());
    assertEquals("EPSG:2154", actual.crs());
    assertFalse(actual.urlsPerGeometry().get(0).isEmpty());
    assertTrue(
        actual.urlsPerGeometry().get(0).stream()
            .allMatch(url -> url.startsWith("https://data.geopf.fr/")));
  }

  private static Geometry an_area_covered_by_swiss_lidar() {
    var coordinates =
        new Coordinate[] {
          new Coordinate(6.220857751344101, 46.218330151666642),
          new Coordinate(6.220048781018293, 46.218321418896565),
          new Coordinate(6.219912346396907, 46.217758267190007),
          new Coordinate(6.221001698608311, 46.217607435687704),
          new Coordinate(6.220857751344101, 46.218330151666642)
        };
    return geometryFactory.createPolygon(coordinates);
  }

  @Test
  void resolves_swiss_lidar_urls() {
    var actual = subject.getLidarFileUrls(List.of(an_area_covered_by_swiss_lidar()));

    log.info("region={} crs={} urls={}", actual.region(), actual.crs(), actual.urlsPerGeometry());
    assertEquals("SWITZERLAND", actual.region());
    assertEquals("EPSG:2056", actual.crs());
    assertFalse(actual.urlsPerGeometry().get(0).isEmpty());
    assertTrue(
        actual.urlsPerGeometry().get(0).stream()
            .allMatch(url -> url.startsWith("https://data.geo.admin.ch/")));
  }

  private static Geometry an_area_covered_by_wallonia_lidar() {
    var coordinates =
        new Coordinate[] {
          new Coordinate(5.578864, 50.629200),
          new Coordinate(5.579732, 50.629187),
          new Coordinate(5.579719, 50.628698),
          new Coordinate(5.578851, 50.628711),
          new Coordinate(5.578864, 50.629200)
        };
    return geometryFactory.createPolygon(coordinates);
  }

  @Test
  void resolves_wallonia_lidar_urls() {
    var actual = subject.getLidarFileUrls(List.of(an_area_covered_by_wallonia_lidar()));

    log.info("region={} crs={} urls={}", actual.region(), actual.crs(), actual.urlsPerGeometry());
    assertEquals("WALLONIA", actual.region());
    assertEquals("EPSG:3812", actual.crs());
    assertFalse(actual.urlsPerGeometry().get(0).isEmpty());
    assertTrue(
        actual.urlsPerGeometry().get(0).stream().allMatch(url -> url.contains("/wallonia/liege/")));
  }
}
