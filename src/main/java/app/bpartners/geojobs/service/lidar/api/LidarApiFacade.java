package app.bpartners.geojobs.service.lidar.api;

import static app.bpartners.geojobs.file.FileWriter.createTempDirectory;
import static app.bpartners.geojobs.service.GeometrySquareMeterArea.EPSG_2056;
import static app.bpartners.geojobs.service.GeometrySquareMeterArea.EPSG_3812;
import static app.bpartners.geojobs.service.GeometrySquareMeterArea.LAMBERT_93;
import static app.bpartners.geojobs.service.GeometrySquareMeterArea.WGS84;
import static java.util.UUID.randomUUID;
import static org.apache.commons.io.FileUtils.deleteQuietly;

import app.bpartners.geojobs.file.FileWriter;
import app.bpartners.geojobs.service.GeometrySquareMeterArea;
import app.bpartners.geojobs.service.cacher.CacherApiClient;
import java.io.File;
import java.net.URI;
import java.net.URL;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.locationtech.jts.geom.Geometry;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

@Component
@AllArgsConstructor
@Slf4j
public class LidarApiFacade {
  private final GeodataLidarApiClient geodataLidarApiClient;
  private final GeometrySquareMeterArea projector;
  private final RestTemplate restTemplate;
  private final CacherApiClient cacherApiClient;

  private static final String LAZ_FILE_SUFFIX = ".laz";

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

  public Optional<File> download(String fileUrl) {
    var directory = createTempDirectory();
    Optional<File> file = Optional.empty();
    try {
      file = download(fileUrl, directory);
      return file;
    } finally {
      if (file.isEmpty()) {
        deleteQuietly(directory);
      }
    }
  }

  @SuppressWarnings("all")
  @SneakyThrows
  private URL getCachedUrl(String fileUrl) {
    return cacherApiClient.getWithCache(new URL(fileUrl));
  }

  @SneakyThrows
  public Optional<File> download(String fileUrl, File directory) {
    log.info("Resolving cached URL for fileUrl={}", fileUrl);
    var cachedUrl = getCachedUrl(fileUrl);
    log.info("Finished resolving cached URL={} for fileUrl={}", cachedUrl, fileUrl);

    log.info("Downloading {}", cachedUrl);
    try {
      var data = restTemplate.getForObject(URI.create(cachedUrl.toString()), byte[].class);
      if (data == null) {
        return Optional.empty();
      }

      var filename = randomUUID().toString();
      var outputPath = Paths.get(directory.getPath(), filename + LAZ_FILE_SUFFIX);
      try {
        FileWriter.write(outputPath, data);
      } catch (Exception e) {
        deleteQuietly(outputPath.toFile());
        throw e;
      }

      log.info("Finished downloading {}", cachedUrl);
      return Optional.of(outputPath.toFile());
    } catch (HttpClientErrorException.NotFound e) {
      log.warn("File not found (404): {}", fileUrl);
      return Optional.empty();
    } catch (Exception e) {
      log.error("Failed to download LAZ file from {}", fileUrl, e);
      throw new RuntimeException("Could not download file: " + fileUrl, e);
    }
  }
}
