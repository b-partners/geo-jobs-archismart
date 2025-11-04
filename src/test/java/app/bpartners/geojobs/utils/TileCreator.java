package app.bpartners.geojobs.utils;

import static java.time.Instant.now;

import app.bpartners.geojobs.endpoint.rest.model.TileCoordinates;
import app.bpartners.geojobs.repository.model.tiling.Tile;

public class TileCreator {
  public Tile create(String tileId, String bucketPath) {
    return Tile.builder()
        .id(tileId)
        .creationDatetime(now())
        .bucketPath(bucketPath)
        .coordinates(new TileCoordinates().x(0).y(0).z(20))
        .build();
  }
}
