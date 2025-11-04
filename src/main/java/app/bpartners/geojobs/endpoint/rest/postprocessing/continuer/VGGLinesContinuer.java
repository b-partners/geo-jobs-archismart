package app.bpartners.geojobs.endpoint.rest.postprocessing.continuer;

import static app.bpartners.geojobs.endpoint.rest.postprocessing.model.TiledPolygon.toTiledPolygons;
import static java.util.stream.Collectors.toSet;

import app.bpartners.geojobs.endpoint.rest.postprocessing.Geojson;
import app.bpartners.geojobs.endpoint.rest.postprocessing.model.TiledPolygon;
import app.bpartners.geojobs.endpoint.rest.postprocessing.model.TilingConf;
import app.bpartners.geojobs.model.geometry.VGG;
import app.bpartners.geojobs.model.geometry.route.RoutesContinuationConf;
import java.util.function.Function;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class VGGLinesContinuer implements Function<VGG, Geojson> {
  private final boolean isZXYDotFiletype;
  private final TilingConf tilingConf;
  private final ParallelTiledLinesContinuer tiledLinesContinuer;

  public VGGLinesContinuer(
      RoutesContinuationConf routesContinuationConf,
      TilingConf tilingConf,
      int neighBoorHoodTileDistance,
      boolean isZXYDotFiletype) {
    this.isZXYDotFiletype = isZXYDotFiletype;
    this.tilingConf = tilingConf;
    this.tiledLinesContinuer =
        new ParallelTiledLinesContinuer(
            routesContinuationConf, tilingConf, neighBoorHoodTileDistance);
  }

  @Override
  public Geojson apply(VGG vgg) {
    var tiledPolygons = toTiledPolygons(tilingConf, vgg, isZXYDotFiletype);
    var latLonPolygonsContinued =
        tiledLinesContinuer.apply(tiledPolygons).stream()
            .map(TiledPolygon::latLonPolygon)
            .collect(toSet());
    return new Geojson(latLonPolygonsContinued);
  }
}
