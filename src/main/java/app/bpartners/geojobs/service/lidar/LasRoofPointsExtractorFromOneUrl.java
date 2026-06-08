package app.bpartners.geojobs.service.lidar;

import static app.bpartners.geojobs.file.FileWriter.createTempDirectory;
import static java.util.stream.Collectors.toSet;

import app.bpartners.geojobs.model.lidar.LasPointGeometry;
import app.bpartners.geojobs.model.lidar.planes.model.DelimitedRoofPoints;
import app.bpartners.geojobs.model.lidar.planes.model.DelimitedRoofPointsItem;
import app.bpartners.geojobs.service.lidar.api.LasIndexApi;
import app.bpartners.geojobs.service.lidar.api.LidarApiFacade;
import com.github.mreutegg.laszip4j.LASReader;
import java.io.File;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiFunction;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.locationtech.jts.geom.Geometry;

@Slf4j
@RequiredArgsConstructor
public class LasRoofPointsExtractorFromOneUrl
    implements BiFunction<String, Set<DelimitedRoofPoints>, Set<DelimitedRoofPoints>> {
  private final LasIndexApi lasIndexApi;
  private final LidarApiFacade lidarApi;
  private final LasFileCleaner lasFileCleaner;

  public LasRoofPointsExtractorFromOneUrl(LidarApiFacade lidarApi, LasIndexApi lasIndexApi) {
    this.lidarApi = lidarApi;
    this.lasIndexApi = lasIndexApi;
    this.lasFileCleaner = new LasFileCleaner();
  }

  private static final short ROOF_LIDAR_CLASS_VALUE = 6;
  private static final short GROUND_LIDAR_CLASS_VALUE = 2;
  private static final short NOT_CLASSIFIED_LIDAR_CLASS_VALUE = 1;
  private static final short DIVERS_BATI_LIDAR_CLASS_VALUE = 67;

  @Override
  public Set<DelimitedRoofPoints> apply(String fileUrl, Set<DelimitedRoofPoints> delimitations) {
    var lasDirectory = createTempDirectory();
    var result = copy(delimitations);
    var optionalFile = downloadLasAndIndexFiles(fileUrl, lasDirectory);
    if (optionalFile.isEmpty()) return Set.of();

    var file = optionalFile.get();
    var lasReader = new LASReader(file);
    var lasHeader = lasReader.getHeader();
    var union = union(delimitations).buffer(2);
    var subReader = getSubReader(union, lasReader);

    log.info("Reading lasPoints from file url: {}", file.getPath());
    for (var point : subReader.getPoints()) {
      var pointClassification = point.getClassification();

      switch (pointClassification) {
        case GROUND_LIDAR_CLASS_VALUE:
          var groundPoint = new LasPointGeometry(point, lasHeader);
          handleGroundPoint(groundPoint, result);
          break;
        case ROOF_LIDAR_CLASS_VALUE,
            DIVERS_BATI_LIDAR_CLASS_VALUE,
            NOT_CLASSIFIED_LIDAR_CLASS_VALUE:
          var roofPoint = new LasPointGeometry(point, lasHeader);
          handleRoofPoint(roofPoint, result);
          break;
        default:
          break;
      }
    }
    log.info("Finished reading lasPoints from: {}", file.getPath());
    clean(file, lasDirectory);
    return result;
  }

  private void clean(File file, File directory) {
    try {
      Files.deleteIfExists(file.toPath());
      this.lasFileCleaner.clean(directory);
    } catch (Exception e) {
      log.error("Failed to clean", e);
    }
  }

  private static LASReader getSubReader(Geometry geometry, LASReader reader) {
    var envelope = geometry.getEnvelopeInternal();
    return reader.insideRectangle(
        envelope.getMinX(), envelope.getMinY(), envelope.getMaxX(), envelope.getMaxY());
  }

  private static Geometry union(Set<DelimitedRoofPoints> delimitations) {
    var geometries =
        delimitations.stream()
            .map(DelimitedRoofPoints::getItems)
            .flatMap(Arrays::stream)
            .map(DelimitedRoofPointsItem::getPolygon)
            .toList();

    Geometry unified = geometries.getFirst();
    for (var geometry : geometries) {
      if (unified == geometry) continue;
      unified = unified.union(geometry);
    }
    return unified;
  }

  private Optional<File> downloadLasAndIndexFiles(String fileUrl, File directory) {
    try {
      var optionalFile = lidarApi.download(fileUrl, directory);
      optionalFile.ifPresent(lasFile -> this.lasIndexApi.download(lasFile, fileUrl));
      return optionalFile;
    } catch (Exception e) {
      log.error("Failed to download lasFile or it's LasIndex fileUrl={}", fileUrl, e);
      throw e;
    }
  }

  private static void handleGroundPoint(
      LasPointGeometry groundPoint, Set<DelimitedRoofPoints> delimitations) {
    for (var delimitation : delimitations) {
      delimitation.addGroundPointIfInside(groundPoint);
    }
  }

  private static void handleRoofPoint(
      LasPointGeometry roofPoint, Set<DelimitedRoofPoints> delimitations) {
    for (var delimitation : delimitations) {
      delimitation.addRoofPointIfInside(roofPoint);
    }
  }

  private static Set<DelimitedRoofPoints> copy(Set<DelimitedRoofPoints> delimitations) {
    return delimitations.stream()
        .map(
            delimitation ->
                delimitation.toBuilder()
                    .groundPoints(new HashSet<>())
                    .items(copyItems(delimitation.getItems()))
                    .build())
        .collect(toSet());
  }

  private static DelimitedRoofPointsItem[] copyItems(DelimitedRoofPointsItem[] items) {
    return Arrays.stream(items)
        .map(item -> item.toBuilder().points(new HashSet<>()).build())
        .toArray(DelimitedRoofPointsItem[]::new);
  }
}
