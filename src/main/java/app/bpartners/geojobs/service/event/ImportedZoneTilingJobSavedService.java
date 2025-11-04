package app.bpartners.geojobs.service.event;

import static app.bpartners.geojobs.job.model.Status.HealthStatus.SUCCEEDED;
import static app.bpartners.geojobs.job.model.Status.ProgressionStatus.FINISHED;
import static app.bpartners.geojobs.model.exception.ApiException.ExceptionType.SERVER_EXCEPTION;
import static app.bpartners.geojobs.repository.model.GeoJobType.TILING;
import static java.time.Instant.now;
import static java.util.UUID.randomUUID;

import app.bpartners.geojobs.endpoint.event.model.zone.ImportedZoneTilingJobSaved;
import app.bpartners.geojobs.endpoint.rest.model.BucketSeparatorType;
import app.bpartners.geojobs.endpoint.rest.model.GeoServerParameter;
import app.bpartners.geojobs.endpoint.rest.model.TileCoordinates;
import app.bpartners.geojobs.file.bucket.CustomBucketComponent;
import app.bpartners.geojobs.job.model.TaskStatus;
import app.bpartners.geojobs.model.exception.ApiException;
import app.bpartners.geojobs.repository.TilingTaskRepository;
import app.bpartners.geojobs.repository.model.Parcel;
import app.bpartners.geojobs.repository.model.ParcelContent;
import app.bpartners.geojobs.repository.model.tiling.ParcelTilingTask;
import app.bpartners.geojobs.repository.model.tiling.Tile;
import app.bpartners.geojobs.repository.model.tiling.ZoneTilingJob;
import app.bpartners.geojobs.service.tiling.ZoneTilingJobService;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import software.amazon.awssdk.services.s3.model.S3Object;

@Service
@AllArgsConstructor
public class ImportedZoneTilingJobSavedService implements Consumer<ImportedZoneTilingJobSaved> {
  public static final int DEFAULT_Z_VALUE = 20;
  private static final String UNDERSCORE = "_";
  private final CustomBucketComponent customBucketComponent;
  private final ZoneTilingJobService tilingJobService;
  private final TilingTaskRepository tilingTaskRepository;

  @Override
  @Transactional
  public void accept(ImportedZoneTilingJobSaved importedZoneTilingJobSaved) {
    var job = tilingJobService.findById(importedZoneTilingJobSaved.getJobId());
    var bucketName = importedZoneTilingJobSaved.getBucketName();
    var bucketPathPrefix = importedZoneTilingJobSaved.getBucketPathPrefix();
    var geoServerParameter = importedZoneTilingJobSaved.getGeoServerParameter();
    var geoServerUrlValue = importedZoneTilingJobSaved.getGeoServerUrl();
    var bucketSeparator =
        importedZoneTilingJobSaved.getBucketSeparatorType() == null
            ? BucketSeparatorType.SLASH
            : importedZoneTilingJobSaved.getBucketSeparatorType();
    List<S3Object> s3Objects =
        getS3Objects(importedZoneTilingJobSaved, bucketName, bucketPathPrefix);

    Map<Integer, List<Tile>> groupedTilesByX = new HashMap<>();
    try {
      groupedTilesByX = getGroupedTiles(s3Objects, bucketSeparator);
    } catch (RuntimeException e) {
      throw new ApiException(SERVER_EXCEPTION, e.getMessage());
    }
    List<ParcelTilingTask> parcelTilingTasks = new ArrayList<>();
    for (Map.Entry<Integer, List<Tile>> entry : groupedTilesByX.entrySet()) {
      List<Tile> groupedTiles = entry.getValue();
      parcelTilingTasks.add(
          getFinishedTasks(job, geoServerUrlValue, geoServerParameter, groupedTiles));
    }
    tilingTaskRepository.saveAll(parcelTilingTasks);
    tilingJobService.recomputeStatus(job);
  }

  @NonNull
  private Map<Integer, List<Tile>> getGroupedTiles(
      List<S3Object> s3Objects, BucketSeparatorType bucketSeparatorType) {
    var tiles =
        s3Objects.subList(1, s3Objects.size()).stream()
            .map(s3Object -> mapFromKey(s3Object.key(), bucketSeparatorType))
            .toList();
    Map<Integer, List<Tile>> groupedByX =
        tiles.stream()
            .filter(tile -> tile.getCoordinates() != null && tile.getCoordinates().getX() != null)
            .collect(Collectors.groupingBy(tile -> tile.getCoordinates().getX()));
    return groupedByX;
  }

  private ParcelTilingTask getFinishedTasks(
      ZoneTilingJob job,
      String geoServerUrlValue,
      GeoServerParameter geoServerParameter,
      List<Tile> groupedTiles) {
    try {
      String jobId = job.getId();
      URL geoServerUrl = new URL(geoServerUrlValue);
      String taskId = randomUUID().toString();
      return ParcelTilingTask.builder()
          .id(taskId)
          .jobId(jobId)
          .statusHistory(
              List.of(
                  TaskStatus.builder()
                      .id(randomUUID().toString())
                      .taskId(taskId)
                      .jobType(TILING)
                      .health(SUCCEEDED)
                      .progression(FINISHED)
                      .creationDatetime(now())
                      .build()))
          .submissionInstant(now())
          .parcels(
              List.of(
                  Parcel.builder()
                      .id(randomUUID().toString())
                      .parcelContent(
                          ParcelContent.builder()
                              .id(randomUUID().toString())
                              .feature(null) // TODO: distinct for each parcels
                              .creationDatetime(now())
                              .geoServerParameter(geoServerParameter)
                              .geoServerUrl(geoServerUrl)
                              .tiles(groupedTiles)
                              .build())
                      .build()))
          .build();
    } catch (MalformedURLException e) {
      throw new ApiException(SERVER_EXCEPTION, e);
    }
  }

  private List<S3Object> getS3Objects(
      ImportedZoneTilingJobSaved importedZoneTilingJobSaved,
      String bucketName,
      String bucketPathPrefix) {
    var defaultS3Objects = customBucketComponent.listObjects(bucketName, bucketPathPrefix);
    var startFromValue = importedZoneTilingJobSaved.getStartFrom();
    var endAtValue = importedZoneTilingJobSaved.getEndAt();
    long startFrom = startFromValue == null ? 0L : startFromValue;
    long endAt = endAtValue == null ? defaultS3Objects.size() : endAtValue;
    if (defaultS3Objects.size() > startFrom || endAt < defaultS3Objects.size()) {
      return new ArrayList<>(defaultS3Objects.subList((int) startFrom, (int) endAt));
    }
    return defaultS3Objects;
  }

  private Tile mapFromKey(String bucketPathKey, BucketSeparatorType bucketSeparatorType) {
    String[] slashSplitter = bucketPathKey.split("/");
    if (slashSplitter.length != 4) {
      throw new ApiException(
          SERVER_EXCEPTION,
          "Unable to convert bucketPathKey " + bucketPathKey + " to TilesCoordinates");
    }
    switch (bucketSeparatorType) {
      case UNDERSCORE -> {
        return mapFromName(bucketPathKey);
      }
      case SLASH -> {
        return Tile.builder()
            .id(randomUUID().toString())
            .bucketPath(bucketPathKey)
            .coordinates(fromBucketPathKey(bucketPathKey))
            .creationDatetime(now())
            .build();
      }
      default ->
          throw new ApiException(
              SERVER_EXCEPTION, "BucketSeparator " + bucketSeparatorType + " unknown");
    }
  }

  private Tile mapFromName(String bucketPath) {
    String[] coordinatesFromPath = bucketPath.split("/");
    String objectName = coordinatesFromPath[3].split(".jpg")[0];
    TileCoordinates coordinates = fromObjectName(objectName);
    return Tile.builder()
        .id(randomUUID().toString())
        .bucketPath(bucketPath)
        .coordinates(coordinates)
        .creationDatetime(now())
        .build();
  }

  public TileCoordinates fromObjectName(String objectName) {
    String[] coordinatesValues = objectName.split(UNDERSCORE);
    if (coordinatesValues.length != 2) {
      throw new ApiException(
          SERVER_EXCEPTION, "Unable to convert objectName " + objectName + " to TilesCoordinates");
    }
    String xValue = coordinatesValues[0];
    String yValue = coordinatesValues[1];
    return new TileCoordinates()
        .x(Integer.valueOf(xValue))
        .y(Integer.valueOf(yValue))
        .z(DEFAULT_Z_VALUE);
  }

  public TileCoordinates fromBucketPathKey(String bucketPathKey) {
    String[] bucketPathValues = bucketPathKey.split("/");
    if (bucketPathValues.length != 4) {
      throw new ApiException(
          SERVER_EXCEPTION,
          "Unable to convert bucketPathKey " + bucketPathKey + " to TilesCoordinates");
    }
    String xValue = bucketPathValues[2];
    String yValue = bucketPathValues[3].split(".jpg")[0];
    String zValue = bucketPathValues[1];
    return new TileCoordinates()
        .x(Integer.valueOf(xValue))
        .y(Integer.valueOf(yValue))
        .z(Integer.valueOf(zValue));
  }
}
