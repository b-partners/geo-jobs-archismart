package app.bpartners.geojobs.service;

import static app.bpartners.geojobs.model.geometry.GeometryFactory.geometryFactory;
import static app.bpartners.geojobs.repository.model.detection.DetectionFileType.TILE_IMAGE;
import static app.bpartners.geojobs.repository.model.detection.DetectionFileType.TILE_MASK;
import static app.bpartners.geojobs.service.geojson.GeometryConverter.unifyMultiPolygon;
import static java.time.Instant.now;
import static java.util.UUID.randomUUID;

import app.bpartners.geojobs.file.bucket.BucketComponent;
import app.bpartners.geojobs.repository.DetectionFileObjectRepository;
import app.bpartners.geojobs.repository.DetectionRepository;
import app.bpartners.geojobs.repository.MachineDetectedTileRepository;
import app.bpartners.geojobs.repository.model.TileDetectionTask;
import app.bpartners.geojobs.repository.model.detection.DetectionFileObject;
import app.bpartners.geojobs.repository.model.detection.FeatureWithDelimitation;
import app.bpartners.geojobs.service.detection.DetectionMapper;
import app.bpartners.geojobs.service.detection.RoofCoveringDetector;
import app.bpartners.geojobs.service.detection.TileObjectDetector;
import app.bpartners.geojobs.service.geojson.GeometryConverter;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.util.List;
import java.util.Objects;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.Polygon;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
@Slf4j
public class TileDetectionTaskConsumer implements TaskConsumer<TileDetectionTask> {
  private final MachineDetectedTileRepository machineDetectedTileRepository;
  private final TileObjectDetector objectsDetector;
  private final DetectionMapper detectionMapper;
  private final DetectionRepository detectionRepository;
  private final GeometryConverter geometryConverter;
  private final DetectionMaskFromTileRetriever maskRetriever;
  private final RoofCoveringDetector roofCoveringDetector;
  private final DetectionFileObjectRepository detectionObjectHistoryRepository;
  private final BucketComponent bucketComponent;

  @SneakyThrows
  @Override
  public void accept(TileDetectionTask tileDetectionTask) {
    var detectableObjectConfigurations = tileDetectionTask.getDetectableObjectConfigurations();
    var zoneDetectionJobId = tileDetectionTask.getZoneDetectionJobId();
    var parcelJobId = tileDetectionTask.getJobId();
    var address = tileDetectionTask.getAddress();
    var point = tileDetectionTask.getPoint();
    var tile = tileDetectionTask.getTile();
    File mask = null;
    var tileCoordinates = tile.getCoordinates();
    var detection = detectionRepository.findByZdjId(zoneDetectionJobId).orElse(null);
    var detectionIdentifier = detection == null ? null : detection.getId();
    tileDetectionTask.setDetectionIdentifier(detectionIdentifier);
    tileDetectionTask.setDebugMode(detection != null && detection.isDebugMode());
    if (detection != null) {
      if (detection.hasToitureModelName()) {
        var multiPolygonFromTile =
            geometryConverter.getMultiPolygonFromTile(
                tileCoordinates.getX(), tileCoordinates.getY(), tileCoordinates.getZ());
        var featureWithDelimitations = detection.getFeatureWithDelimitations();
        var roofMultiPolygonIntersectedWithTilePolygon =
            featureWithDelimitations.stream()
                .map(FeatureWithDelimitation::delimitations)
                .flatMap(List::stream)
                .map(
                    roofFeature -> {
                      var geometry =
                          geometryConverter.readGeometryFromString(
                              roofFeature.getGeometry().getActualInstanceStringValue());
                      if (geometry instanceof MultiPolygon roofMultiPolygon) {
                        return multiPolygonFromTile.intersection(roofMultiPolygon);
                      }
                      if (geometry instanceof Polygon roofPolygon) {
                        return multiPolygonFromTile.intersection(roofPolygon);
                      }
                      return null;
                    })
                .map(
                    geometry -> {
                      if (geometry instanceof MultiPolygon multiPolygon) {
                        return multiPolygon;
                      }
                      if (geometry instanceof Polygon polygon) {
                        return geometryFactory.createMultiPolygon(new Polygon[] {polygon});
                      }
                      return null;
                    })
                .filter(Objects::nonNull)
                .toList();
        if (!roofMultiPolygonIntersectedWithTilePolygon.isEmpty()) {
          var maskMultiPolygon =
              roofMultiPolygonIntersectedWithTilePolygon.stream()
                  .reduce(unifyMultiPolygon())
                  .orElse(null);
          mask = maskRetriever.apply(tile, maskMultiPolygon);
        } else {
          log.info(
              "Actual multiPolygon retrieved from tile {} not intersecting with any roof"
                  + " multiPolygon",
              multiPolygonFromTile);
          return;
        }
      }
      var tileImageBucketPath = tile.getBucketPath();
      if (detection.isDebugMode()) {
        detectionObjectHistoryRepository.save(
            DetectionFileObject.builder()
                .id(randomUUID().toString())
                .detectionIdentifier(detectionIdentifier)
                .fileName(replaceSeparator(tileImageBucketPath))
                .bucketKey(tileImageBucketPath)
                .fileType(TILE_IMAGE)
                .creationDatetime(now())
                .build());
      }
      if (mask == null && detection.hasToitureModelName()) {
        log.info(
            "Actual tile detection could not compute mask, skipping detection for TileDetectionTask"
                + " : {}",
            tileDetectionTask);
        return;
      }
      if (detection.isDebugMode()) {
        var maskBucketKey = toMaskPath(tileImageBucketPath);
        bucketComponent.upload(mask, maskBucketKey);
        detectionObjectHistoryRepository.save(
            DetectionFileObject.builder()
                .id(randomUUID().toString())
                .detectionIdentifier(detectionIdentifier)
                .fileName(replaceSeparator(maskBucketKey))
                .bucketKey(maskBucketKey)
                .fileType(TILE_MASK)
                .creationDatetime(now())
                .build());
      }
    }
    var detectionResponse =
        objectsDetector.apply(tileDetectionTask, mask, detectableObjectConfigurations);

    var roofCoveringResponse =
        roofCoveringDetector.apply(
            tile.toBuilder()
                .detectionE2Id(detection != null ? detection.getEndToEndId() : null)
                .build(),
            mask);
    var machineDetectedTile =
        detectionMapper.toDetectedTile(
            detectionResponse,
            tile,
            tileDetectionTask.getParcelId(),
            zoneDetectionJobId,
            parcelJobId);
    if (roofCoveringResponse != null) {
      machineDetectedTile.setPrimaryRoofCovering(roofCoveringResponse.primary());
      machineDetectedTile.setSecondaryRoofCovering(roofCoveringResponse.secondary());
    }

    if (machineDetectedTile.getDetectedObjects() != null) {
      machineDetectedTile
          .getDetectedObjects()
          .forEach(
              detectedObject -> {
                if (address != null) {
                  detectedObject.getFeature().getProperties().put("address", address);
                }
                if (point != null) {
                  try {
                    var pointAsJson =
                        new ObjectMapper().findAndRegisterModules().writeValueAsString(point);
                    detectedObject.getFeature().getProperties().put("point", pointAsJson);
                  } catch (JsonProcessingException e) {
                    throw new RuntimeException(e);
                  }
                }
              });
    }
    machineDetectedTileRepository.save(machineDetectedTile);
  }

  private static String toMaskPath(String path) {
    return insertSuffix(path, "_mask");
  }

  private static String insertSuffix(String path, String suffix) {
    if (path == null) {
      return null;
    }
    int dotIndex = path.lastIndexOf('.');
    if (dotIndex < 0) {
      return path + suffix;
    }
    return path.substring(0, dotIndex) + suffix + path.substring(dotIndex);
  }

  private static String replaceSeparator(String path) {
    if (path == null) {
      return null;
    }
    return path.replace('/', '_');
  }
}
