package app.bpartners.geojobs.service;

import app.bpartners.geojobs.repository.model.tiling.Tile;
import app.bpartners.geojobs.service.detection.DetectionMaskCreator;
import java.io.File;
import java.util.function.BiFunction;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.locationtech.jts.geom.MultiPolygon;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DetectionMaskFromTileRetriever implements BiFunction<Tile, MultiPolygon, File> {
  private final DetectionMaskCreator maskCreator;
  private final TileCoordinatesPolygonIntersection tilePolygonIntersection;

  @Override
  public File apply(Tile tile, MultiPolygon roofMultiPolygon) {
    var tileCoordinates = tile.getCoordinates();
    var projectorPixels = tilePolygonIntersection.intersects(roofMultiPolygon, tileCoordinates);
    return maskCreator.apply(projectorPixels);
  }
}
