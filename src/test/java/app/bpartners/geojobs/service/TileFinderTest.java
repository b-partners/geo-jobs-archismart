package app.bpartners.geojobs.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import app.bpartners.geojobs.endpoint.rest.model.TileCoordinates;
import app.bpartners.geojobs.service.tiling.TileFinder;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

class TileFinderTest {
  TileFinder subject = new TileFinder();

  @Test
  void get_surrounding_tiles() {
    var latitude = BigDecimal.valueOf(46.651930);
    var longitude = BigDecimal.valueOf(-0.249212);
    var zoom = 20;
    var expectedSurroundingTiles = expectedCoordinates();

    var actual = subject.getSurroundingTiles(longitude, latitude, zoom);

    assertEquals(expectedSurroundingTiles, actual);
  }

  private List<TileCoordinates> expectedCoordinates() {
    var zoom = 20;
    return List.of(
        new TileCoordinates().x(523561).y(370292).z(zoom),
        new TileCoordinates().x(523562).y(370292).z(zoom),
        new TileCoordinates().x(523563).y(370292).z(zoom),
        new TileCoordinates().x(523561).y(370293).z(zoom),
        new TileCoordinates().x(523562).y(370293).z(zoom),
        new TileCoordinates().x(523563).y(370293).z(zoom),
        new TileCoordinates().x(523561).y(370294).z(zoom),
        new TileCoordinates().x(523562).y(370294).z(zoom),
        new TileCoordinates().x(523563).y(370294).z(zoom));
  }
}
