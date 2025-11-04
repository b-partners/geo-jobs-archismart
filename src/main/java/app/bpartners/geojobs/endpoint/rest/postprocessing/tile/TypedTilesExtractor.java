package app.bpartners.geojobs.endpoint.rest.postprocessing.tile;

import static java.time.Instant.now;
import static java.util.UUID.randomUUID;
import static java.util.stream.Collectors.toSet;

import app.bpartners.geojobs.endpoint.rest.model.DetectableObjectType;
import app.bpartners.geojobs.endpoint.rest.model.DetectedParcel;
import app.bpartners.geojobs.endpoint.rest.model.DetectedTile;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Predicate;

public class TypedTilesExtractor
    implements BiFunction<DetectedParcel, Predicate<DetectableObjectType>, Set<DetectedTile>> {
  @Override
  public Set<DetectedTile> apply(
      DetectedParcel parcel, Predicate<DetectableObjectType> detectableTypePredicate) {
    return parcel.getDetectedTiles().stream()
        .map(t -> filter(t, detectableTypePredicate))
        .collect(toSet());
  }

  private DetectedTile filter(
      DetectedTile t, Predicate<DetectableObjectType> detectableTypePredicate) {
    var res = new DetectedTile();
    res.setTileId(randomUUID().toString());
    res.setCreationDatetime(now());

    res.setStatus(t.getStatus());
    res.setBucketPath(t.getBucketPath());

    res.setDetectedObjects(
        t.getDetectedObjects().stream()
            .filter(o -> detectableTypePredicate.test(o.getDetectedObjectType()))
            .toList());
    return res;
  }
}
