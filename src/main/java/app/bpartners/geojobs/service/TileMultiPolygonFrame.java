package app.bpartners.geojobs.service;

import static app.bpartners.geojobs.model.geometry.GeometryFactory.geometryFactory;
import static app.bpartners.geojobs.repository.model.ArcgisImageZoom.HOUSES_0;

import app.bpartners.geojobs.service.geojson.GeometryConverter;
import app.bpartners.geojobs.service.tiling.TileFinder;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;
import lombok.RequiredArgsConstructor;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.Polygon;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TileMultiPolygonFrame
    implements BiFunction<BigDecimal, BigDecimal, Optional<MultiPolygon>> {
  private final TileFinder tileFinder;
  private final GeometryConverter geometryConverter;

  @Override
  public Optional<MultiPolygon> apply(BigDecimal longitude, BigDecimal latitude) {
    var tileCoordinates =
        tileFinder.getSurroundingTiles(longitude, latitude, HOUSES_0.getZoomLevel());
    return tileCoordinates.stream()
        .map(
            tileCoordinate ->
                geometryConverter.getMultiPolygonFromTile(
                    tileCoordinate.getX(), tileCoordinate.getY(), tileCoordinate.getZ()))
        .reduce(
            (multiPolygon1, multiPolygon2) -> {
              var unifiedGeometry = multiPolygon1.union(multiPolygon2);
              if (unifiedGeometry instanceof MultiPolygon multiPolygon) {
                return multiPolygon;
              } else if (unifiedGeometry instanceof org.locationtech.jts.geom.Polygon polygon) {
                return geometryFactory.createMultiPolygon(
                    new org.locationtech.jts.geom.Polygon[] {polygon});
              }
              throw new UnsupportedOperationException(
                  "Unsupported unified geometry : " + unifiedGeometry);
            });
  }

  public Optional<MultiPolygon> apply(Object geometry) {
    Optional<MultiPolygon> frame;
    switch (geometry) {
      case MultiPolygon jtsMultiPolygon -> {
        var centroid = geometryConverter.centroidFromGeometry(jtsMultiPolygon);
        var longitude = centroid.getFirst();
        var latitude = centroid.getLast();
        frame = apply(longitude, latitude);
      }
      case Polygon jtsPolygon -> {
        var centroid = geometryConverter.centroidFromGeometry(jtsPolygon);
        var longitude = centroid.getFirst();
        var latitude = centroid.getLast();
        frame = apply(longitude, latitude);
      }
      case app.bpartners.geojobs.endpoint.rest.model.MultiPolygon restMultiPolygon -> {
        var jtsMultiPolygon = geometryConverter.apply(restMultiPolygon.getCoordinates());
        var centroid = geometryConverter.centroidFromGeometry(jtsMultiPolygon);
        var longitude = centroid.getFirst();
        var latitude = centroid.getLast();
        frame = apply(longitude, latitude);
      }
      case app.bpartners.geojobs.endpoint.rest.model.Polygon restPolygon -> {
        var jtsMultiPolygon = geometryConverter.apply(List.of(restPolygon.getCoordinates()));
        var centroid = geometryConverter.centroidFromGeometry(jtsMultiPolygon);
        var longitude = centroid.getFirst();
        var latitude = centroid.getLast();
        frame = apply(longitude, latitude);
      }
      default -> throw new IllegalArgumentException("Unsupported geometry : " + geometry);
    }
    return frame;
  }
}
