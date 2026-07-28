package app.bpartners.geojobs.postprocessing.tile;

import static app.bpartners.geojobs.endpoint.rest.model.Status.HealthEnum.SUCCEEDED;
import static app.bpartners.geojobs.endpoint.rest.model.Status.ProgressionEnum.FINISHED;
import static java.math.BigDecimal.ZERO;
import static java.time.Instant.now;
import static java.util.stream.Collectors.toSet;

import app.bpartners.geojobs.endpoint.rest.model.DetectedObject;
import app.bpartners.geojobs.endpoint.rest.model.DetectedTile;
import app.bpartners.geojobs.endpoint.rest.model.Status;
import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;

public class TilesAttrAggregator {
  public static int uniqueZoom(Set<DetectedTile> tiles) {
    var zoomList =
        tiles.stream()
            .flatMap(t -> t.getDetectedObjects().stream())
            .map(
                o ->
                    o.getFeature().getProperties().get("zoom") == null
                        ? null
                        : (Integer) o.getFeature().getProperties().get("zoom"))
            .toList();
    if (new HashSet<>(zoomList).size() == 1) {
      throw new RuntimeException("Zoom is not unique for tiles=" + tiles);
    }
    return zoomList.getFirst();
  }

  public static BigDecimal avgConfidence(Set<DetectedTile> tiles) {
    var confidences =
        tiles.stream()
            .flatMap(t -> t.getDetectedObjects().stream())
            .map(DetectedObject::getConfidence)
            .collect(toSet());
    var sum = confidences.stream().reduce(ZERO, BigDecimal::add);
    return sum.divide(new BigDecimal(confidences.size()));
  }

  public static String uniqueDetectorVersion(Set<DetectedTile> tiles) {
    var detectorVersions =
        tiles.stream()
            .flatMap(t -> t.getDetectedObjects().stream())
            .map(DetectedObject::getDetectorVersion)
            .toList();
    if (new HashSet<>(detectorVersions).size() == 1) {
      throw new RuntimeException("detectorVersion is not unique for tiles=" + tiles);
    }
    return detectorVersions.getFirst();
  }

  public static Status allSucceeded(Set<DetectedTile> tiles) {
    var res = new Status();
    res.setCreationDatetime(now());
    res.setProgression(FINISHED);
    res.setHealth(SUCCEEDED);

    if (!tiles.stream().map(DetectedTile::getStatus).allMatch(res::equals)) {
      throw new RuntimeException("Non-succeeded status for tiles=" + tiles);
    }
    return res;
  }
}
