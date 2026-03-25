package app.bpartners.geojobs.service.event;

import static java.time.Instant.now;
import static java.util.UUID.randomUUID;

import app.bpartners.geojobs.endpoint.rest.model.GeoServerParameter;
import app.bpartners.geojobs.endpoint.rest.model.TileCoordinates;
import app.bpartners.geojobs.endpoint.rest.model.TileInfoSize;
import app.bpartners.geojobs.file.bucket.BucketComponent;
import app.bpartners.geojobs.file.zip.FileUnzipper;
import app.bpartners.geojobs.job.model.Status;
import app.bpartners.geojobs.repository.model.ParcelContent;
import app.bpartners.geojobs.repository.model.tiling.ParcelTilingTask;
import app.bpartners.geojobs.repository.model.tiling.Tile;
import app.bpartners.geojobs.service.TaskConsumer;
import app.bpartners.geojobs.service.tiling.downloader.TilesDownloader;
import java.io.File;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@AllArgsConstructor
@Component
@Slf4j
public class TilingTaskConsumer implements TaskConsumer<ParcelTilingTask> {
  private static final int DEFAULT_TILE_SIZE = 1024;
  private final TilesDownloader tilesDownloader;
  private final BucketComponent bucketComponent;

  @Override
  public void accept(ParcelTilingTask parcelTilingTask) {
    var tilingTaskStart = now();
    var parcel = parcelTilingTask.getParcelContent();

    File downloadedTiles = tilesDownloader.apply(parcel);
    String bucketKey = downloadedTiles.getName();

    bucketComponent.upload(downloadedTiles, bucketKey);
    downloadedTiles.delete();

    setParcelTiles(downloadedTiles, parcel, bucketKey);
    log.info(
        "Tiling task finished in {} seconds for tiles={}",
        Duration.between(tilingTaskStart, now()).toSeconds(),
        parcelTilingTask.getTiles().stream()
            .map(
                tile ->
                    "(x="
                        + tile.getCoordinates().getX()
                        + ", y="
                        + tile.getCoordinates().getY()
                        + ", z="
                        + tile.getCoordinates().getZ()
                        + ")")
            .collect(Collectors.joining(",")));
  }

  private void setParcelTiles(File tilesDir, ParcelContent parcelContent, String bucketKey) {
    var serverParameter = parcelContent.getGeoServerParameter();
    List<Tile> extractedTiles = getParcelTiles(serverParameter, tilesDir, bucketKey);
    parcelContent.setTiles(extractedTiles);
  }

  private List<Tile> getParcelTiles(
      GeoServerParameter serverParameter, File tilesFile, String bucketKey) {
    List<Tile> accumulator = new ArrayList<>();
    var tileWidth = serverParameter == null ? DEFAULT_TILE_SIZE : serverParameter.getWidth();
    var tileHeight = serverParameter == null ? DEFAULT_TILE_SIZE : serverParameter.getHeight();

    if (!tilesFile.isDirectory()) {
      var enrichedAccumulator = new ArrayList<>(accumulator);

      String entryParentPath = tilesFile.getPath();
      String[] dir = entryParentPath.split("/");
      var x = Integer.valueOf(dir[dir.length - 2]);
      var z = Integer.valueOf(dir[dir.length - 3]);
      var y = Integer.valueOf(FileUnzipper.stripExtension(tilesFile.getName()));
      String[] segments = entryParentPath.split("/");
      String filePath = "";

      if (segments.length >= 3) {
        filePath =
            "/"
                + segments[segments.length - 3]
                + "/"
                + segments[segments.length - 2]
                + "/"
                + segments[segments.length - 1];
      }

      Tile extractedTile =
          Tile.builder()
              .id(randomUUID().toString())
              .size(new TileInfoSize().width(tileWidth).height(tileHeight))
              .creationDatetime(now())
              .coordinates(new TileCoordinates().x(x).y(y).z(z))
              .bucketPath(bucketKey + filePath)
              .build();
      enrichedAccumulator.add(extractedTile);

      return enrichedAccumulator;
    }

    return Arrays.stream(tilesFile.listFiles())
        .flatMap(subFile -> getParcelTiles(serverParameter, subFile, bucketKey).stream())
        .collect(Collectors.toList());
  }

  public static ParcelTilingTask withNewStatus(
      ParcelTilingTask task,
      Status.ProgressionStatus progression,
      Status.HealthStatus health,
      String message) {
    return (ParcelTilingTask)
        task.hasNewStatus(
            Status.builder()
                .progression(progression)
                .health(health)
                .creationDatetime(now())
                .message(message)
                .build());
  }
}
