package app.bpartners.geojobs.utils.lidar;

import static app.bpartners.geojobs.file.FileWriter.createTempDirectory;
import static app.bpartners.geojobs.service.GeometrySquareMeterArea.LAMBERT_93;
import static app.bpartners.geojobs.service.GeometrySquareMeterArea.WGS84;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toSet;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import app.bpartners.geojobs.service.GeometrySquareMeterArea;
import app.bpartners.geojobs.service.lidar.LasFileCleaner;
import app.bpartners.geojobs.service.lidar.LasRoofPointsExtractorFromOneUrl;
import app.bpartners.geojobs.service.lidar.LasRoofsPointsExtractor;
import app.bpartners.geojobs.service.lidar.api.LasIndexApi;
import app.bpartners.geojobs.service.lidar.api.LidarApiFacade;
import app.bpartners.geojobs.service.lidar.api.SwissBoundaryChecker;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.*;
import org.locationtech.jts.geom.Geometry;

public class LasRoofsPointsExtractorCreator {
  private static final GeometrySquareMeterArea projector = new GeometrySquareMeterArea();

  public static LasRoofsPointsExtractor create(LidarApiFacade lidarApi) {
    return new LasRoofsPointsExtractor(
        lasIndexApiMock(), lidarApi, projector, swissBoundaryCheckerMock());
  }

  public static LasRoofsPointsExtractor create(String url, Set<Geometry> geometries) {
    return create(Map.of(url, geometries));
  }

  public static LasRoofsPointsExtractor create(Set<String> urls, Set<Geometry> geometries) {
    Map<String, Set<Geometry>> result = new HashMap<>();
    for (var url : urls) {
      result.putIfAbsent(url, geometries);
    }
    return create(result);
  }

  public static LasRoofsPointsExtractor create(Map<String, Set<Geometry>> geometries) {
    var projected = new HashMap<String, Set<Geometry>>();

    geometries.forEach(
        (key, value) -> {
          var projectedGeometries =
              value.stream()
                  .map(geometry -> projector.project(geometry, WGS84, LAMBERT_93))
                  .collect(toSet());
          projected.put(key, projectedGeometries);
        });

    var filesUrl = new ArrayList<>(geometries.keySet());
    var filesData =
        filesUrl.stream().map(LasRoofsPointsExtractorCreator::getDuplicatedResourceFile).toList();

    var lidarApiMock = mock(LidarApiFacade.class);
    when(lidarApiMock.getUniqueLidarFilesUrls(any())).thenReturn(projected);
    when(lidarApiMock.download(any(), any()))
        .thenAnswer(
            invocation -> {
              var filename = invocation.getArguments()[0].toString();
              return Optional.of(filesData.get(filesUrl.indexOf(filename)));
            });

    return new LasRoofsPointsExtractor(
        lidarApiMock, projector, swissBoundaryCheckerMock(), fromOneUrl(lidarApiMock));
  }

  private static LasRoofPointsExtractorFromOneUrl fromOneUrl(LidarApiFacade lidarApi) {
    var lasFileCleaner = lasFileCleanerMock();
    var lasIndexApi = lasIndexApiMock();
    return new LasRoofPointsExtractorFromOneUrl(lasIndexApi, lidarApi, lasFileCleaner);
  }

  private static LasIndexApi lasIndexApiMock() {
    var lasIndexApi = mock(LasIndexApi.class);
    when(lasIndexApi.download(any(), any())).thenReturn(Optional.empty());
    return lasIndexApi;
  }

  private static LasFileCleaner lasFileCleanerMock() {
    var cleaner = mock(LasFileCleaner.class);
    doNothing().when(cleaner).clean(any());
    return cleaner;
  }

  private static SwissBoundaryChecker swissBoundaryCheckerMock() {
    var checker = mock(SwissBoundaryChecker.class);
    when(checker.isGeometryInSwiss(any())).thenReturn(false);
    return checker;
  }

  public static File getDuplicatedResourceFile(String path) {
    try {
      File source = getResourceFile(path);

      File copy = new File(createTempDirectory(), source.getName());

      Files.copy(source.toPath(), copy.toPath(), StandardCopyOption.REPLACE_EXISTING);

      return copy;
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public static File getResourceFile(String path) {
    return new File(
        requireNonNull(LasRoofsPointsExtractorCreator.class.getClassLoader().getResource(path))
            .getFile());
  }
}
