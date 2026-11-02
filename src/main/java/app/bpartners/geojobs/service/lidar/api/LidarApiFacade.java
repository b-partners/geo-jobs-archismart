package app.bpartners.geojobs.service.lidar.api;

import static app.bpartners.geojobs.service.GeometrySquareMeterArea.EPSG_2056;
import static app.bpartners.geojobs.service.GeometrySquareMeterArea.EPSG_3812;
import static app.bpartners.geojobs.service.GeometrySquareMeterArea.LAMBERT_93;
import static app.bpartners.geojobs.service.GeometrySquareMeterArea.WGS84;

import app.bpartners.geojobs.service.GeometrySquareMeterArea;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.locationtech.jts.geom.Geometry;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
@Slf4j
public class LidarApiFacade {
  private final GeodataLidarApiClient geodataLidarApiClient;
  private final GeometrySquareMeterArea projector;

  public record LidarFilesResult(
      Map<String, Set<Geometry>> filesUrls, CoordinateReferenceSystem targetCrs) {}

  public LidarFilesResult getUniqueLidarFilesUrls(Collection<Geometry> wgs84Geometries) {
    var geometriesList = List.copyOf(wgs84Geometries);
    if (geometriesList.isEmpty()) {
      return new LidarFilesResult(Map.of(), LAMBERT_93);
    }

    var result = geodataLidarApiClient.getLidarFileUrls(geometriesList);
    var targetCrs = resolveCrs(result.region());

    Map<String, Set<Geometry>> filesUrls = new HashMap<>();
    for (int i = 0; i < geometriesList.size(); i++) {
      var wgs84Geometry = geometriesList.get(i);
      var targetGeometry = projector.project(wgs84Geometry, WGS84, targetCrs);
      var urls =
          i < result.urlsPerGeometry().size() ? result.urlsPerGeometry().get(i) : Set.<String>of();

      for (var url : urls) {
        log.info("LAZ File to download: {}", url);
        filesUrls.computeIfAbsent(url, key -> new HashSet<>()).add(targetGeometry);
      }
    }

    return new LidarFilesResult(filesUrls, targetCrs);
  }

  static CoordinateReferenceSystem resolveCrs(String region) {
    return switch (region) {
      case "SWITZERLAND" -> EPSG_2056;
      case "WALLONIA" -> EPSG_3812;
      default -> LAMBERT_93;
    };
  }
}
