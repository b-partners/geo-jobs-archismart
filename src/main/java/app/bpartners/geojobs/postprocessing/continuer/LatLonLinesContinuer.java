package app.bpartners.geojobs.postprocessing.continuer;

import static app.bpartners.geojobs.postprocessing.BoundaryMerger.invert;
import static java.util.stream.Collectors.toSet;

import app.bpartners.geojobs.model.geometry.route.RoutesContinuationConf;
import app.bpartners.geojobs.postprocessing.GeoJsonLoader;
import app.bpartners.geojobs.postprocessing.model.LatLonPolygon;
import app.bpartners.geojobs.postprocessing.model.TiledPolygon;
import app.bpartners.geojobs.postprocessing.model.TilingConf;
import java.io.File;
import java.util.Set;

public final class LatLonLinesContinuer extends LinesContinuer<LatLonPolygon> {
  private final GeoJsonLoader geoJsonLoader;

  private final ParallelTiledLinesContinuer parallelTiledLinesContinuer;

  public LatLonLinesContinuer(
      RoutesContinuationConf routesContinuationConf,
      TilingConf tilingConf,
      int neighboorHoodThreshold) {
    this.parallelTiledLinesContinuer =
        new ParallelTiledLinesContinuer(routesContinuationConf, tilingConf, neighboorHoodThreshold);
    this.geoJsonLoader = new GeoJsonLoader();
  }

  @Override
  public Set<LatLonPolygon> apply(Set<LatLonPolygon> latLonPolygons) {
    var tilingConf = parallelTiledLinesContinuer.getTiledLinesContinuer().tilingConf();
    var tiledPolygons =
        latLonPolygons.stream().map(p -> p.tiledPolygon(tilingConf)).collect(toSet());
    var continuedTiledPolygons = parallelTiledLinesContinuer.apply(tiledPolygons);
    return continuedTiledPolygons.stream().map(TiledPolygon::latLonPolygon).collect(toSet());
  }

  public Set<LatLonPolygon> apply(File geojsonPath) {
    var latLons = geoJsonLoader.apply(geojsonPath);
    var inverted = invert(latLons);
    return apply(inverted);
  }
}
