package app.bpartners.geojobs.service.geojson;

import static app.bpartners.geojobs.endpoint.rest.postprocessing.model.TiledPolygon.toLatLon;

import app.bpartners.geojobs.endpoint.rest.postprocessing.model.TilingConf;
import app.bpartners.geojobs.model.geometry.IntXY;
import java.math.BigDecimal;
import java.util.List;

public class GeoReferencer {

  private GeoReferencer() {}

  public static List<BigDecimal> toGeographicalCoordinates(
      int xTile, int yTile, double x, double y, int zoom, int imageWidth) {
    var originTile = new IntXY(xTile, yTile);
    var tilingConf = new TilingConf(zoom, imageWidth);
    var pixel = new IntXY((int) x, (int) y);
    var coordinate = toLatLon(originTile, tilingConf, pixel);
    return List.of(BigDecimal.valueOf(coordinate.y), BigDecimal.valueOf(coordinate.x));
  }
}
