package app.bpartners.geojobs.endpoint.rest.postprocessing.continuer;

import static app.bpartners.geojobs.endpoint.rest.model.DetectableObjectType.LINE;
import static app.bpartners.geojobs.endpoint.rest.postprocessing.model.TiledPolygon.newTiledPolygons;
import static app.bpartners.geojobs.endpoint.rest.postprocessing.tile.TilesAttrAggregator.allSucceeded;
import static app.bpartners.geojobs.endpoint.rest.postprocessing.tile.TilesAttrAggregator.avgConfidence;
import static app.bpartners.geojobs.endpoint.rest.postprocessing.tile.TilesAttrAggregator.uniqueDetectorVersion;
import static app.bpartners.geojobs.endpoint.rest.postprocessing.tile.TilesAttrAggregator.uniqueZoom;
import static java.time.Instant.now;
import static java.util.UUID.randomUUID;
import static java.util.function.Predicate.not;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;

import app.bpartners.geojobs.endpoint.rest.model.DetectedObject;
import app.bpartners.geojobs.endpoint.rest.model.DetectedParcel;
import app.bpartners.geojobs.endpoint.rest.model.DetectedTile;
import app.bpartners.geojobs.endpoint.rest.model.Feature;
import app.bpartners.geojobs.endpoint.rest.model.FeatureGeometry;
import app.bpartners.geojobs.endpoint.rest.model.Status;
import app.bpartners.geojobs.endpoint.rest.postprocessing.model.DetectionAttr;
import app.bpartners.geojobs.endpoint.rest.postprocessing.model.LatLonPolygon;
import app.bpartners.geojobs.endpoint.rest.postprocessing.model.TiledPolygon;
import app.bpartners.geojobs.endpoint.rest.postprocessing.model.TilingConf;
import app.bpartners.geojobs.endpoint.rest.postprocessing.tile.TypedTilesExtractor;
import app.bpartners.geojobs.model.geometry.route.Route;
import app.bpartners.geojobs.model.geometry.route.RoutesContinuation;
import app.bpartners.geojobs.model.geometry.route.RoutesContinuationConf;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class DetectedLinesContinuer implements Function<DetectedParcel, DetectedParcel> {

  private final RoutesContinuationConf routesContinuationConf;
  private final TypedTilesExtractor typedTilesExtractor;
  private final int imgSize; // TODO: should be given by DetectedTile.TileInfo

  public DetectedLinesContinuer(RoutesContinuationConf routesContinuationConf, int imgSize) {
    this.routesContinuationConf = routesContinuationConf;
    this.typedTilesExtractor = new TypedTilesExtractor();
    this.imgSize = imgSize;
  }

  @Override
  public DetectedParcel apply(DetectedParcel parcel) {
    try {
      return fallibleApply(parcel);
    } catch (Exception e) {
      log.error("Doing nothing as route continuation failed, parcel={}", parcel, e);
      return parcel;
    }
  }

  private DetectedParcel fallibleApply(DetectedParcel parcel) {
    var tileWithContinuedRoutes =
        tilesWithContinuedRoutes(typedTilesExtractor.apply(parcel, LINE::equals));
    var tilesWithNonRoutes = typedTilesExtractor.apply(parcel, not(LINE::equals));
    var newTiles =
        Stream.concat(tilesWithNonRoutes.stream(), Stream.of(tileWithContinuedRoutes))
            .collect(toList());
    return newDetectedParcel(newTiles, parcel);
  }

  private DetectedTile tilesWithContinuedRoutes(Set<DetectedTile> routes) {
    var tiledRoutes = newTiledPolygons(routes, imgSize);
    var routesContinuation =
        new RoutesContinuation(
            tiledRoutes.stream().map(t -> new Route(t.polygon(), t.type())).collect(toSet()),
            routesContinuationConf);
    var continuedRoutes = routesContinuation.continued();

    var z = uniqueZoom(routes);
    var tilingConf = new TilingConf(z, imgSize);
    var tiledContinuedRoutes = tiledPolygons(continuedRoutes, tilingConf);

    return continuedTile(
        tiledContinuedRoutes,
        new DetectionAttr(LINE, uniqueDetectorVersion(routes), avgConfidence(routes)),
        allSucceeded(routes));
  }

  private static Set<TiledPolygon> tiledPolygons(
      Set<org.locationtech.jts.geom.Polygon> jtsPolygons, TilingConf tilingConf) {
    return jtsPolygons.stream()
        .map(LatLonPolygon::new)
        .map(p -> p.tiledPolygon(tilingConf))
        .collect(toSet());
  }

  private DetectedTile continuedTile(
      Set<TiledPolygon> tiledContinuedRoutes, DetectionAttr detectionAttr, Status status) {
    var tileWithContinuedRoutes = new DetectedTile();
    tileWithContinuedRoutes.setTileId(newId());
    tileWithContinuedRoutes.setCreationDatetime(now());
    tileWithContinuedRoutes
        // continuedRoutes come from multiple tiles,
        // hence multiple images, hence multiple bucketPath...
        // ... unless we compute union of these images
        // and upload result in new bucketPath
        .setBucketPath(null);

    tileWithContinuedRoutes.setStatus(status);

    tileWithContinuedRoutes.setDetectedObjects(
        continuedRoutes(tiledContinuedRoutes, detectionAttr));
    return tileWithContinuedRoutes;
  }

  private List<DetectedObject> continuedRoutes(
      Set<TiledPolygon> continuedTiledRoutes, DetectionAttr detectionAttr) {
    return continuedTiledRoutes.stream().map(p -> continuedRoute(p, detectionAttr)).toList();
  }

  private DetectedObject continuedRoute(TiledPolygon tiledPolygon, DetectionAttr detectionAttr) {
    var res = new DetectedObject();
    res.setDetectedObjectType(detectionAttr.objectType());
    res.setConfidence(detectionAttr.confidence());
    res.setDetectorVersion(detectionAttr.detectorVersion());
    res.setFeature(newFeature(tiledPolygon));
    return res;
  }

  private Feature newFeature(TiledPolygon tiledPolygon) {
    var feature = new Feature();
    feature.getProperties().put("id", randomUUID().toString());
    feature.getProperties().put("zoom", tiledPolygon.tilingConf().z());

    var featureGeometry = new FeatureGeometry();
    featureGeometry.setActualInstance(tiledPolygon.latLonPolygon().polygon());

    return feature;
  }

  private static String newId() {
    return randomUUID().toString();
  }

  private DetectedParcel newDetectedParcel(
      List<DetectedTile> newTiles, DetectedParcel originalParcel) {
    var res = new DetectedParcel();
    res.setId(newId());
    res.setCreationDatetime(now());

    res.setParcelId(originalParcel.getParcelId());
    res.setStatus(originalParcel.getStatus());
    res.setDetectionJobIb(originalParcel.getDetectionJobIb());

    res.setDetectedTiles(newTiles);
    return res;
  }
}
