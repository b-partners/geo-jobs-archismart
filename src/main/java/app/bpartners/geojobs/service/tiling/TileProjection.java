package app.bpartners.geojobs.service.tiling;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Polygon;
import org.springframework.stereotype.Component;

@Component
public class TileProjection {

  public Coordinate tilePixelToLonLat(double x, double y, int tileX, int tileY, int zoom) {
    double tileSize = 1024.0;
    double worldSize = tileSize * Math.pow(2, zoom);

    double pixelX = tileX * tileSize + x;
    double pixelY = tileY * tileSize + y;

    double lon = (pixelX / worldSize) * 360.0 - 180.0;

    double n = Math.PI - (2.0 * Math.PI * pixelY) / worldSize;
    double lat = Math.toDegrees(Math.atan(Math.sinh(n)));

    return new Coordinate(lon, lat);
  }

  public Polygon pixelPolygonToGeo(Polygon pixelPolygon, int tileX, int tileY, int zoom) {
    GeometryFactory factory = new GeometryFactory();

    Coordinate[] coords = pixelPolygon.getCoordinates();
    Coordinate[] transformed = new Coordinate[coords.length];

    for (int i = 0; i < coords.length; i++) {
      transformed[i] = tilePixelToLonLat(coords[i].x, coords[i].y, tileX, tileY, zoom);
    }
    return factory.createPolygon(transformed);
  }
}
