package app.bpartners.geojobs.endpoint.rest.postprocessing.model;

import static app.bpartners.geojobs.endpoint.rest.postprocessing.model.LatLonPolygon.originTile;
import static app.bpartners.geojobs.endpoint.rest.postprocessing.model.LatLonPolygon.toPixel;
import static org.junit.jupiter.api.Assertions.assertEquals;

import app.bpartners.geojobs.model.geometry.IntXY;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;

class LatLonPolygonTest {

  @Test
  void origin_tile() {
    var actual = originTile(new Coordinate(45.82129270556462, 4.899666979908943), 20);
    assertEquals(new IntXY(538_559, 373_791), actual);
  }

  @Test
  void to_pixel() {
    var actual =
        toPixel(new LatLon(45.82129270556462, 4.899666979908943), new TilingConf(20, 1_024));
    assertEquals(new IntXY(322, 385), actual);
  }
}
