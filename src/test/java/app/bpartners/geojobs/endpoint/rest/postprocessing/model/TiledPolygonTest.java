package app.bpartners.geojobs.endpoint.rest.postprocessing.model;

import static app.bpartners.geojobs.endpoint.rest.postprocessing.model.TiledPolygon.toLatLon;
import static org.junit.jupiter.api.Assertions.assertEquals;

import app.bpartners.geojobs.model.geometry.IntXY;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;

class TiledPolygonTest {

  @Test
  void to_lat_lon() {
    var originTile = new IntXY(538_559, 373_791);
    var pixel = new IntXY(322, 385);
    var actual = toLatLon(originTile, new TilingConf(20, 1_024), pixel);

    assertEquals(new Coordinate(45.82129270556462, 4.899666979908943), actual);
  }
}
