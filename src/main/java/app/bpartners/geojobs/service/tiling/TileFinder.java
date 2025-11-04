package app.bpartners.geojobs.service.tiling;

import app.bpartners.geojobs.endpoint.rest.model.TileCoordinates;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.locationtech.jts.geom.*;
import org.springframework.stereotype.Component;

@Component
public class TileFinder {

  private int lonToTileX(double lon, int zoom) {
    return (int) Math.floor((lon + 180) / 360 * Math.pow(2, zoom));
  }

  private int latToTileY(double lat, int zoom) {
    return (int)
        Math.floor(
            (1
                    - Math.log(Math.tan(Math.toRadians(lat)) + 1 / Math.cos(Math.toRadians(lat)))
                        / Math.PI)
                / 2
                * Math.pow(2, zoom));
  }

  private double tileXToLon(int x, int zoom) {
    return x / Math.pow(2.0, zoom) * 360.0 - 180;
  }

  private double tileYToLat(int y, int zoom) {
    double n = Math.PI - 2.0 * Math.PI * y / Math.pow(2.0, zoom);
    return Math.toDegrees(Math.atan(Math.sinh(n)));
  }

  public List<TileCoordinates> getFromGeoJsonPolygon(Geometry polygon, int zoom) {
    Envelope envelope = polygon.getEnvelopeInternal();

    int minX = lonToTileX(envelope.getMinX(), zoom);
    int maxX = lonToTileX(envelope.getMaxX(), zoom);
    int minY = latToTileY(envelope.getMaxY(), zoom);
    int maxY = latToTileY(envelope.getMinY(), zoom);

    List<TileCoordinates> intersectingTiles = new ArrayList<>();
    GeometryFactory geometryFactory = new GeometryFactory();

    for (int x = minX; x <= maxX; x++) {
      for (int y = minY; y <= maxY; y++) {
        double west = tileXToLon(x, zoom);
        double east = tileXToLon(x + 1, zoom);
        double north = tileYToLat(y, zoom);
        double south = tileYToLat(y + 1, zoom);

        Polygon tilePolygon =
            geometryFactory.createPolygon(
                new Coordinate[] {
                  new Coordinate(west, south),
                  new Coordinate(east, south),
                  new Coordinate(east, north),
                  new Coordinate(west, north),
                  new Coordinate(west, south)
                });

        if (polygon.intersects(tilePolygon)) {
          intersectingTiles.add(new TileCoordinates().z(zoom).x(x).y(y));
        }
      }
    }
    return intersectingTiles;
  }

  public List<TileCoordinates> getSurroundingTiles(BigDecimal lon, BigDecimal lat, int zoom) {
    int x = lonToTileX(lon.doubleValue(), zoom);
    int y = latToTileY(lat.doubleValue(), zoom);

    List<TileCoordinates> tileCoordinates = new ArrayList<>();
    for (int dx = -1; dx <= 1; dx++) {
      for (int dy = -1; dy <= 1; dy++) {
        tileCoordinates.add(new TileCoordinates().x(x + dx).y(y + dy).z(zoom));
      }
    }
    tileCoordinates.sort(
        Comparator.comparing(TileCoordinates::getZ)
            .thenComparing(TileCoordinates::getY)
            .thenComparing(TileCoordinates::getX));
    return tileCoordinates;
  }
}
