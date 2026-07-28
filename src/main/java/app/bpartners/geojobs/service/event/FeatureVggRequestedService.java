package app.bpartners.geojobs.service.event;

import static java.time.Instant.now;

import app.bpartners.geojobs.endpoint.event.EventProducer;
import app.bpartners.geojobs.endpoint.event.model.FeatureAnnotatedImageRequested;
import app.bpartners.geojobs.endpoint.event.model.FeatureVggRequested;
import app.bpartners.geojobs.endpoint.rest.model.*;
import app.bpartners.geojobs.model.geometry.VGGFactory;
import app.bpartners.geojobs.repository.DetectionRepository;
import app.bpartners.geojobs.repository.MachineDetectedTileRepository;
import app.bpartners.geojobs.repository.model.detection.DetectableObjectConfiguration;
import app.bpartners.geojobs.repository.model.detection.Detection;
import app.bpartners.geojobs.service.*;
import jakarta.persistence.EntityManager;
import java.time.Duration;
import java.util.List;
import java.util.function.Consumer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class FeatureVggRequestedService implements Consumer<FeatureVggRequested> {
  private static final int DEFAULT_TILE_SIZE = 1024;
  private final EntityManager entityManager;
  private final DetectionRepository detectionRepository;
  private final MachineDetectedTileRepository detectedTileRepository;
  private final VGGFactory vggFactory;
  private final DetectionVGGUpdate detectionVGGUpdate;
  private final TileCoordinatesService tileCoordinatesService;
  private final TiledPixelPolygonComputer tiledPixelPolygonComputer;
  private final FeaturePolygonRetriever featurePolygonRetriever;
  private final EventProducer eventProducer;

  @Override
  public void accept(FeatureVggRequested event) {
    entityManager.clear();
    var detectionIdentifier = event.getDetectionIdentifier();
    var currentFeature = event.getFeature();
    var detection = detectionRepository.findById(detectionIdentifier).orElseThrow();

    apply(detection, currentFeature);
  }

  public Detection apply(Detection detection, Feature currentFeature) {
    var featureVggComputationStart = now();
    if (!detection.hasToitureModelName()) {
      log.error("Only BP_TOITURE model is supported to generated VGG from now");
      return null;
    }
    var geoJsonDelimitationType = detection.getGeoJsonDelimitationType();
    var currentFeaturePolygonGeoJson =
        featurePolygonRetriever.apply(currentFeature, geoJsonDelimitationType);
    if (currentFeaturePolygonGeoJson == null) {
      return null;
    }
    var detectableTypes =
        detection.getDetectableObjectConfigurations().stream()
            .map(DetectableObjectConfiguration::getObjectType)
            .toList();
    var machineDetectedTiles = detectedTileRepository.findAllByZdjJobId(detection.getZdjId());
    var delimitationsFeatures = detection.getDelimitationOf(currentFeature);
    var tiledPixelPolygons =
        tiledPixelPolygonComputer.apply(
            currentFeaturePolygonGeoJson,
            delimitationsFeatures,
            detectableTypes,
            machineDetectedTiles,
            detection.hasParcelDelimitationType());

    var completedQuadrilateralTileCoordinates =
        tileCoordinatesService.computeFeatureTileCoordinatesWithCompleteQuadrilateral(
            currentFeature, geoJsonDelimitationType);

    var vggMap = vggFactory.from(tiledPixelPolygons, completedQuadrilateralTileCoordinates);

    var newDetection =
        detectionVGGUpdate.apply(vggMap.values(), detection, retrieveId(currentFeature));

    var tilesColNumbers = tileCoordinatesService.colNumbers(completedQuadrilateralTileCoordinates);
    var tileRowNumbers = tileCoordinatesService.rowNumbers(completedQuadrilateralTileCoordinates);
    var imageWidth = tilesColNumbers * DEFAULT_TILE_SIZE;
    var imageHeight = tileRowNumbers * DEFAULT_TILE_SIZE;

    var savedDetection =
        detectionRepository.save(
            newDetection.toBuilder().imageWidth(imageWidth).imageHeight(imageHeight).build());

    log.info(
        "VGG computation finished in {} seconds for detection(e2Id={}) and feature(geometry={})",
        Duration.between(featureVggComputationStart, now()).toSeconds(),
        detection.getEndToEndId(),
        currentFeature.getGeometry());
    if (detection.isDebugMode()) {
      eventProducer.accept(List.of(new FeatureAnnotatedImageRequested(savedDetection.getId())));
    }
    return savedDetection;
  }

  private String retrieveId(Feature feature) {
    var featureId =
        feature.getProperties().get("feature_id") == null
            ? null
            : feature.getProperties().get("feature_id").toString();
    return feature.getProperties().get("id") == null
        ? featureId
        : feature.getProperties().get("id").toString();
  }
}
