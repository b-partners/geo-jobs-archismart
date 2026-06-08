package app.bpartners.geojobs.service.event;

import static app.bpartners.geojobs.repository.model.ArcgisImageZoom.HOUSES_0;
import static javax.imageio.ImageIO.read;

import app.bpartners.geojobs.endpoint.event.model.FeatureImageRequested;
import app.bpartners.geojobs.file.WhiteImageDetector;
import app.bpartners.geojobs.file.bucket.BucketComponent;
import app.bpartners.geojobs.model.exception.NotImplementedException;
import app.bpartners.geojobs.repository.DetectionRepository;
import app.bpartners.geojobs.repository.TilingTaskRepository;
import app.bpartners.geojobs.repository.model.tiling.ParcelTilingTask;
import app.bpartners.geojobs.service.*;
import app.bpartners.geojobs.service.geojson.GeometryConverter;
import app.bpartners.geojobs.service.tiling.TileFinder;
import jakarta.persistence.EntityManager;
import java.util.List;
import java.util.function.Consumer;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class FeatureImageRequestedService implements Consumer<FeatureImageRequested> {
  private static final int ONE_KILOMETRE_SQUARE_AREA = 1_000_000;
  private final DetectionRepository detectionRepository;
  private final GeometryConverter geometryConverter;
  private final BucketComponent bucketComponent;
  private final TileImagesAssembler tileImagesAssembler;
  private final TilingTaskRepository tilingTaskRepository;
  private final GeometrySquareMeterArea geometrySquareMeterArea;
  private final TileImageBlur tileImageBlur;
  private final WhiteImageDetector whiteImageDetector;
  private final TileFinder tileFinder;
  private final EntityManager entityManager;
  private final TileDuplicationRemover tileDuplicationRemover;
  private final DetectionPolygonProcessedRetriever polygonProcessedRetriever;

  @SneakyThrows
  @Override
  public void accept(FeatureImageRequested event) {
    entityManager.clear();
    var feature = event.getFeature();
    var detectionIdentifier = event.getDetectionIdentifier();
    var detection = detectionRepository.findById(detectionIdentifier).orElseThrow();
    var providedGeoJsonZone = detection.getProvidedGeoJsonZone();
    var zoom =
        providedGeoJsonZone.getFirst().getProperties().get("zoom") == null
            ? HOUSES_0.getZoomLevel()
            : (Integer) providedGeoJsonZone.getFirst().getProperties().get("zoom");

    var zonePolygonGeometryProcessed =
        polygonProcessedRetriever.apply(feature, detection.getGeoJsonDelimitationType());
    if (zonePolygonGeometryProcessed == null) return;
    var actualArea = geometrySquareMeterArea.apply(zonePolygonGeometryProcessed);
    if (actualArea > ONE_KILOMETRE_SQUARE_AREA) {
      log.warn(
          "Feature image requested not implemented for zone over 1km^2, otherwise actual provided"
              + " polygon is {} for detection.id={} and feature {}",
          actualArea,
          detectionIdentifier,
          feature);
      return;
    }
    // TODO : paginate finding tilingTasks to optimize performance
    var tilingJobIdentifier = detection.getZtjId();
    var tilingTasks = tilingTaskRepository.findAllByJobId(tilingJobIdentifier);
    var tileCoordinatesEnvelopingPolygon =
        tileFinder.getFromGeoJsonPolygon(zonePolygonGeometryProcessed, zoom);

    var tiles =
        tilingTasks.stream()
            .map(ParcelTilingTask::getTiles)
            .flatMap(List::stream)
            .filter(tile -> tileCoordinatesEnvelopingPolygon.contains(tile.getCoordinates()))
            .toList();
    var deDuplicatedTiles = tileDuplicationRemover.apply(tiles);
    var tilesWithImages =
        deDuplicatedTiles.stream()
            .map(
                tile ->
                    tile.toBuilder().image(bucketComponent.download(tile.getBucketPath())).build())
            .toList();
    var tilesWithBlur = tileImageBlur.apply(zonePolygonGeometryProcessed, tilesWithImages);
    var assembleImageFile = tileImagesAssembler.apply(tilesWithBlur);

    if (whiteImageDetector.apply(read(assembleImageFile))) {
      throw new NotImplementedException(
          "Address " + detection.getZoneName() + " not supported for now");
    }

    if (detection.getProvidedGeoJsonZone() != null
        && detection.getProvidedGeoJsonZone().size() == 1) {
      bucketComponent.upload(assembleImageFile, "zone_images/" + detection.getId() + ".jpg");
    }
    bucketComponent.upload(
        assembleImageFile,
        "zone_images/"
            + detection.getId()
            + "/"
            + feature.getProperties().get("id")
            + "/"
            + detection.getZoneName()
            + ".jpg");
  }
}
