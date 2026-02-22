package app.bpartners.geojobs.service.lidar.api;

import static app.bpartners.geojobs.service.GeometrySquareMeterArea.LAMBERT_93;
import static app.bpartners.geojobs.service.GeometrySquareMeterArea.WGS84;
import static java.util.stream.Collectors.toSet;

import app.bpartners.geojobs.service.GeometrySquareMeterArea;
import java.io.File;
import java.io.FileOutputStream;
import java.util.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

@Component
@AllArgsConstructor
@Slf4j
public class LidarApiFacade {
  private final IgnLidarApi ignLidarApi;
  private final OpenSourceLidarApi openSourceLidarApi;
  private final FallbackLidarApi fallbackLidarApi;

  private final RestTemplate restTemplate;
  private final GeometrySquareMeterArea projector;

  private static final long UPDATED_VALID_DATA = 50_000_000;
  private static final String UPDATED_DATA_PREFIX = "https://data.geopf.fr/";
  private static final Set<String> ALLOWED_URL_PREFIXES =
      Set.of(
          "https://storage.sbg.cloud.ovh.net/", "https://lidar.data.gouv.fr/", UPDATED_DATA_PREFIX);

  public Map<String, Set<Geometry>> getUniqueLidarFilesUrls(Collection<Geometry> wgs84Geometries) {
    Set<ProjectedGeometry> geometries = getProjectedGeometries(wgs84Geometries);
    Map<String, Set<Geometry>> filesUrls = new HashMap<>();

    for (var geometry : geometries) {
      var urls = getLidarFilesUrls(geometry.wgs84().getEnvelopeInternal(), openSourceLidarApi);

      if (urls.isEmpty()) {
        log.info("No LidarFiles found from the OpenSourceLidarAPI, using IGN as fallback");
        urls = getLidarFilesUrls(geometry.lambert93().getEnvelopeInternal(), ignLidarApi);
        urls = handleDeprecatedData(geometry.lambert93().getEnvelopeInternal(), urls);
      }

      for (var url : urls) {
        log.info("LAZ File to download: {}", url);
        filesUrls.computeIfAbsent(url, key -> new HashSet<>()).add(geometry.lambert93());
      }
    }

    return filesUrls;
  }

  public Optional<File> download(String fileUrl) {
    if (!isSafeUrl(fileUrl)) {
      log.warn("Unsafe URL blocked: {}", fileUrl);
      return Optional.empty();
    }

    log.info("Downloading {}", fileUrl);
    try {
      byte[] data = restTemplate.getForObject(fileUrl, byte[].class);
      if (data == null) {
        return Optional.empty();
      }

      var tempFile = File.createTempFile("lidar-", ".laz");
      try (var outputStream = new FileOutputStream(tempFile)) {
        outputStream.write(data);
      }

      log.info("Finished downloading {}", fileUrl);
      return Optional.of(tempFile);
    } catch (HttpClientErrorException.NotFound e) {
      log.warn("File not found (404): {}", fileUrl);
      return Optional.empty();
    } catch (Exception e) {
      log.error("Failed to download LAZ file from {}", fileUrl, e);
      throw new RuntimeException("Could not download file: " + fileUrl, e);
    }
  }

  private Set<String> handleDeprecatedData(Envelope envelope, Set<String> urls) {
    if (urls.stream().allMatch(url -> url.startsWith(UPDATED_DATA_PREFIX))) {
      return urls;
    }

    var hasValidContent = urls.stream().allMatch(this::hasValidContent);
    if (hasValidContent) {
      return urls;
    }

    return fallbackLidarApi.getUniqueLidarUrlsForDeprecatedData(envelope);
  }

  private boolean hasValidContent(String url) {
    try {
      if (!isSafeUrl(url)) {
        log.warn("Unsafe URL blocked for HEAD: {}", url);
        return false;
      }

      var headers = restTemplate.headForHeaders(url);
      long contentLength = headers.getContentLength();
      log.info("Content-Length={} for fileUrl={}", contentLength, url);
      return contentLength > UPDATED_VALID_DATA;
    } catch (Exception e) {
      log.warn("Cannot get the Content-Length of the fileUrl={}", url);
      return true;
    }
  }

  private Set<ProjectedGeometry> getProjectedGeometries(Collection<Geometry> wgs84Geometries) {
    return wgs84Geometries.stream()
        .map(
            wgs84Geometry -> {
              var lambert93 = projector.project(wgs84Geometry, WGS84, LAMBERT_93);
              return new ProjectedGeometry(lambert93, wgs84Geometry);
            })
        .collect(toSet());
  }

  private static Set<String> getLidarFilesUrls(Envelope envelope, LidarApi api) {
    return api.apply(envelope).stream().filter(LidarApiFacade::isSafeUrl).collect(toSet());
  }

  private static boolean isSafeUrl(String url) {
    if (url == null) return false;
    return ALLOWED_URL_PREFIXES.stream().anyMatch(url::startsWith);
  }

  @Builder
  private record ProjectedGeometry(Geometry lambert93, Geometry wgs84) {}
}
