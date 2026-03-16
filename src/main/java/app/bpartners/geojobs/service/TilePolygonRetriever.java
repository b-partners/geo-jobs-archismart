package app.bpartners.geojobs.service;

import static app.bpartners.geojobs.repository.model.ArcgisImageZoom.HOUSES_0;

import app.bpartners.geojobs.repository.model.ArcgisImageZoom;
import app.bpartners.geojobs.service.geojson.GeometryConverter;
import app.bpartners.geojobs.service.tiling.TileFinder;
import java.util.List;
import java.util.function.Function;
import lombok.RequiredArgsConstructor;
import org.locationtech.jts.geom.Polygon;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TilePolygonRetriever implements Function<Polygon, List<Polygon>> {
  private final TileFinder tileFinder;
  private final GeometryConverter geometryConverter;

  @Override
  public List<Polygon> apply(Polygon geometryPolygon) {
    return apply(geometryPolygon, HOUSES_0);
  }

  public List<Polygon> apply(Polygon geometryPolygon, ArcgisImageZoom imageZoom) {
    var tileCoordinatesFromPolygon =
        tileFinder.getFromGeoJsonPolygon(geometryPolygon, imageZoom.getZoomLevel());
    return tileCoordinatesFromPolygon.stream()
        .map(
            coordinates ->
                geometryConverter.getMultiPolygonFromTile(
                    coordinates.getX(), coordinates.getY(), coordinates.getZ()))
        .map(multiPolygon -> (org.locationtech.jts.geom.Polygon) multiPolygon.getGeometryN(0))
        .toList();
  }
}
