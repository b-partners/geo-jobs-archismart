package app.bpartners.geojobs.service.cityjson.texture;

import lombok.extern.slf4j.Slf4j;
import org.locationtech.jts.geom.*;

// TODO: refactor : use existing code
@Slf4j
public class Converter {
  public static Coordinate lonLatToPixelInTile(
      Coordinate coordinate, int tileX, int tileY, int zoom, int tileSizePx) {
    double n = Math.pow(2.0, zoom);
    double x = (coordinate.getX() + 180.0) / 360.0 * n;
    double latRad = Math.toRadians(coordinate.getY());
    double y = (1.0 - Math.log(Math.tan(latRad) + 1.0 / Math.cos(latRad)) / Math.PI) / 2.0 * n;
    double pixelX = (x - tileX) * tileSizePx;
    double pixelY = (y - tileY) * tileSizePx;
    return new Coordinate(pixelX, pixelY);
  }
}
