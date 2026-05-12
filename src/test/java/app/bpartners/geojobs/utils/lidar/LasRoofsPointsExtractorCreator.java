package app.bpartners.geojobs.utils.lidar;

import static app.bpartners.geojobs.file.FileWriter.createTempDirectory;
import static app.bpartners.geojobs.service.GeometrySquareMeterArea.LAMBERT_93;
import static app.bpartners.geojobs.service.GeometrySquareMeterArea.WGS84;
import static java.nio.file.Files.readAllBytes;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toSet;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import app.bpartners.geojobs.file.ExtensionGuesser;
import app.bpartners.geojobs.file.FileWriter;
import app.bpartners.geojobs.service.GeometrySquareMeterArea;
import app.bpartners.geojobs.service.lidar.LasRoofsPointsExtractor;
import app.bpartners.geojobs.service.lidar.api.LidarApiFacade;
import app.bpartners.geojobs.service.lidar.api.SwissBoundaryChecker;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.util.*;
import lombok.SneakyThrows;
import org.locationtech.jts.geom.Geometry;

public class LasRoofsPointsExtractorCreator {
  private static final GeometrySquareMeterArea projector = new GeometrySquareMeterArea();
  private static final FileWriter fileWriter =
      new FileWriter(new ObjectMapper(), new ExtensionGuesser());

  private static SwissBoundaryChecker swissBoundaryCheckerMock() {
    var checker = mock(SwissBoundaryChecker.class);
    when(checker.isGeometryInSwiss(any())).thenReturn(false);

    return checker;
  }

  public static LasRoofsPointsExtractor create(LidarApiFacade lidarApi) {
    return new LasRoofsPointsExtractor(lidarApi, projector, swissBoundaryCheckerMock());
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
        filesUrl.stream().map(LasRoofsPointsExtractorCreator::createTempFileFromResources).toList();

    var lidarApiMock = mock(LidarApiFacade.class);
    when(lidarApiMock.getUniqueLidarFilesUrls(any())).thenReturn(projected);

    when(lidarApiMock.download(any()))
        .thenAnswer(
            invocation -> {
              var filename = invocation.getArguments()[0].toString();
              return Optional.of(filesData.get(filesUrl.indexOf(filename)));
            });

    return new LasRoofsPointsExtractor(lidarApiMock, projector, swissBoundaryCheckerMock());
  }

  @SneakyThrows
  public static File createTempFileFromResources(String path) {
    var lasFileFromResource =
        new File(
            requireNonNull(LasRoofsPointsExtractorCreator.class.getClassLoader().getResource(path))
                .getFile());
    return fileWriter.write(
        readAllBytes(lasFileFromResource.toPath()),
        createTempDirectory(),
        lasFileFromResource.getName());
  }
}
