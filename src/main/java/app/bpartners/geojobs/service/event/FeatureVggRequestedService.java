package app.bpartners.geojobs.service.event;

import static app.bpartners.geojobs.endpoint.rest.controller.mapper.FeatureMapper.toRestFeature;
import static app.bpartners.geojobs.endpoint.rest.model.Detection.GeoJsonDelimitationTypeEnum.PARCEL;
import static app.bpartners.geojobs.endpoint.rest.model.Feature.TypeEnum.FEATURE;
import static app.bpartners.geojobs.endpoint.rest.model.Polygon.TypeEnum.POLYGON;
import static app.bpartners.geojobs.repository.model.ArcgisImageZoom.HOUSES_0;
import static app.bpartners.geojobs.service.geojson.GeometryConverter.getMultiPolygonZoneProcessed;
import static java.time.Instant.now;

import app.bpartners.geojobs.endpoint.event.model.FeatureVggRequested;
import app.bpartners.geojobs.endpoint.rest.controller.mapper.FeatureMapper;
import app.bpartners.geojobs.endpoint.rest.model.*;
import app.bpartners.geojobs.endpoint.rest.model.Detection;
import app.bpartners.geojobs.model.geometry.PolygonObjectType;
import app.bpartners.geojobs.model.geometry.TiledPixelPolygon;
import app.bpartners.geojobs.model.geometry.VGGFactory;
import app.bpartners.geojobs.repository.DetectionRepository;
import app.bpartners.geojobs.repository.MachineDetectedTileRepository;
import app.bpartners.geojobs.repository.model.detection.*;
import app.bpartners.geojobs.repository.model.detection.DetectableObjectConfiguration;
import app.bpartners.geojobs.service.DetectionVGGUpdate;
import app.bpartners.geojobs.service.PolygonCoordinatesCloser;
import app.bpartners.geojobs.service.TileCoordinatesPolygonIntersection;
import app.bpartners.geojobs.service.geojson.GeometryConverter;
import app.bpartners.geojobs.service.ign.IgnCadastreFeatureFetcher;
import app.bpartners.geojobs.service.tiling.TileFinder;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.*;
import java.util.function.Consumer;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.locationtech.jts.geom.Polygon;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class FeatureVggRequestedService implements Consumer<FeatureVggRequested> {
  private final DetectionRepository detectionRepository;
  private final MachineDetectedTileRepository detectedTileRepository;
  private final VGGFactory vggFactory;
  private final GeometryConverter geometryConverter;
  private final DetectionVGGUpdate detectionVGGUpdate;
  private final PolygonCoordinatesCloser polygonCoordinatesCloser;
  private final TileCoordinatesPolygonIntersection tileCoordinatesPolygonIntersection;
  private final FeatureMapper featureMapper;
  private final DetectionRoofPropertiesRequestedService detectionRoofPropertiesRequestedService;
  private final TileFinder tileFinder;
  private final EntityManager entityManager;
  private final IgnCadastreFeatureFetcher ignCadastreFeatureFetcher;

  @Override
  public void accept(FeatureVggRequested event) {
    var featureVggComputationStart = now();
    entityManager.clear();
    var detectionIdentifier = event.getDetectionIdentifier();
    var feature = event.getFeature();
    var detection = detectionRepository.findById(detectionIdentifier).orElseThrow();
    if (!detection.hasToitureModelName()) {
      log.error("Only BP_TOITURE model is supported to generated VGG from now");
      return;
    }
    var machineDetectedTiles = detectedTileRepository.findAllByZdjJobId(detection.getZdjId());
    var featureWithDelimitationList = detection.getFeatureWithDelimitations();
    var actualDelimitation =
        featureWithDelimitationList.stream()
            .filter(
                f ->
                    f.getRestFeature() != null
                        && f.getRestFeature().getGeometry() != null
                        && f.getRestFeature().getGeometry().equals(feature.getGeometry()))
            .findFirst()
            .orElse(
                featureWithDelimitationList.size() == 1
                        && featureWithDelimitationList.getFirst().getRestDelimitations() != null
                        && featureWithDelimitationList.getFirst().getRestDelimitations().size() == 1
                    ? featureWithDelimitationList.getFirst()
                    : null);
    if (actualDelimitation == null) {
      throw new NoSuchElementException("No delimitation found for " + feature.getGeometry());
    }
    var featureDelimitationWithRoofProperties =
        detectionRoofPropertiesRequestedService.applyRoofPropertiesOnDelimitation(
            machineDetectedTiles, actualDelimitation);
    var polygonGeoJson =
        getPolygonGeoJsonFromFeature(feature, detection.getGeoJsonDelimitationType());
    if (polygonGeoJson == null) return;
    var detectableTypes =
        detection.getDetectableObjectConfigurations().stream()
            .map(DetectableObjectConfiguration::getObjectType)
            .toList();
    var latLonRoofFeatures = featureDelimitationWithRoofProperties.getRestDelimitations();
    var tiledPixelPolygons =
        getTiledPixelPolygon(
            polygonGeoJson,
            latLonRoofFeatures,
            detectableTypes,
            machineDetectedTiles,
            detection.hasParcelDelimitationType());

    var tileCoordinatesRetrievedFromFeature =
        retrieveFeatureTileCoordinates(feature, detection.getGeoJsonDelimitationType());
    var completedQuadrilateralCoordinates =
        completeToFullQuadrilateral(tileCoordinatesRetrievedFromFeature);

    var vggMap = vggFactory.from(tiledPixelPolygons, completedQuadrilateralCoordinates);

    var newDetection = detectionVGGUpdate.apply(vggMap.values(), detection, event.getFeatureNb());

    detectionRepository.save(newDetection);
    log.info(
            "VGG computation finished in {} seconds for detection(e2Id={}) and feature(geometry={})",
            Duration.between(featureVggComputationStart, now()).toSeconds(),
            detection.getEndToEndId(),
            feature.getGeometry());

  }

  private Feature getPolygonGeoJsonFromFeature(
      Feature feature, Detection.GeoJsonDelimitationTypeEnum delimitationTypeEnum) {
    Feature polygonGeoJsonZone;
    var geometryInstance = feature.getGeometry().getActualInstance();
    switch (geometryInstance) {
      case Point point -> {
        List<List<List<List<BigDecimal>>>> geometryMultiPolygonCoordinates;
        if (PARCEL.equals(delimitationTypeEnum)) {
          geometryMultiPolygonCoordinates = retrieveParcelMultiPolygonCoordinates(point);
        } else {
          geometryMultiPolygonCoordinates =
              geometryConverter.multiPolygonToNestedList(
                  geometryConverter.retrieveNearestRoofMultiPolygon(point));
        }
        if (geometryMultiPolygonCoordinates.size() > 1) {
          log.error(
              "MultiPolygon roof with more than one polygon is not supported, only the first one is"
                  + " taken");
        }
        polygonGeoJsonZone =
            new Feature()
                .type(FEATURE)
                .properties(feature.getProperties())
                .geometry(
                    new FeatureGeometry(
                        new app.bpartners.geojobs.endpoint.rest.model.Polygon()
                            .type(POLYGON)
                            .coordinates(geometryMultiPolygonCoordinates.getFirst())));
      }
      case app.bpartners.geojobs.endpoint.rest.model.Polygon ignored -> {
        polygonGeoJsonZone = feature;
      }
      case MultiPolygon multiPolygon -> {
        if (multiPolygon.getCoordinates().size() > 1) {
          log.error(
              "Provided multiPolygon with more than one polygon is not supported, only the first"
                  + " one is taken");
        }
        polygonGeoJsonZone =
            new Feature()
                .type(FEATURE)
                .properties(feature.getProperties())
                .geometry(
                    new FeatureGeometry(
                        new app.bpartners.geojobs.endpoint.rest.model.Polygon()
                            .type(POLYGON)
                            .coordinates(multiPolygon.getCoordinates().getFirst())));
      }
      default -> {
        log.error(
            "Unexpected geometry type: {} aborting vgg computing for feature {}",
            geometryInstance,
            feature);
        return null;
      }
    }
    return polygonGeoJsonZone;
  }

  @SneakyThrows
  private List<List<List<List<BigDecimal>>>> retrieveParcelMultiPolygonCoordinates(Point point) {
    var parcelsNearestPoint =
        ignCadastreFeatureFetcher.apply(
            geometryConverter.readGeometryFromString(new ObjectMapper().writeValueAsString(point)));
    if (parcelsNearestPoint.size() > 1) {
      log.warn(
          "More than one parcel found for point {}, used first {}",
          point,
          parcelsNearestPoint.getFirst());
    }
    return convertParcelToGeometryMultiPolygonCoordinates(parcelsNearestPoint);
  }

  private List<List<List<List<BigDecimal>>>> convertParcelToGeometryMultiPolygonCoordinates(
      List<app.bpartners.geojobs.repository.model.Feature> parcelsNearestPoint) {
    var restFeature = toRestFeature(parcelsNearestPoint.getFirst());
    var actualParcelInstance = restFeature.getGeometry().getActualInstance();
    switch (actualParcelInstance) {
      case app.bpartners.geojobs.endpoint.rest.model.Polygon polygon -> {
        return List.of(polygon.getCoordinates());
      }
      case MultiPolygon multiPolygon -> {
        return multiPolygon.getCoordinates();
      }
      default ->
          throw new IllegalStateException("Unexpected geometry type: " + actualParcelInstance);
    }
  }

  private List<TileCoordinates> retrieveFeatureTileCoordinates(
      Feature feature, Detection.GeoJsonDelimitationTypeEnum delimitationTypeEnum) {
    var polygonGeometry =
        geometryConverter.retrieveZonePolygonGeometryProcessed(feature, delimitationTypeEnum);
    return tileFinder.getFromGeoJsonPolygon(polygonGeometry, HOUSES_0.getZoomLevel()).stream()
        .sorted(
            Comparator.comparing(TileCoordinates::getZ)
                .thenComparing(TileCoordinates::getY)
                .thenComparing(TileCoordinates::getX))
        .toList();
  }

  // TODO: handle inside specific class and method ?
  private List<TileCoordinates> completeToFullQuadrilateral(List<TileCoordinates> tiles) {
    if (tiles == null || tiles.size() <= 2) {
      return tiles;
    }
    int zoom = tiles.getFirst().getZ();
    int minX = tiles.stream().mapToInt(TileCoordinates::getX).min().orElseThrow();
    int maxX = tiles.stream().mapToInt(TileCoordinates::getX).max().orElseThrow();
    int minY = tiles.stream().mapToInt(TileCoordinates::getY).min().orElseThrow();
    int maxY = tiles.stream().mapToInt(TileCoordinates::getY).max().orElseThrow();

    int cols = (maxX - minX) + 1;
    int rows = (maxY - minY) + 1;
    int expectedTileCount = cols * rows;
    if (tiles.size() == expectedTileCount) {
      return tiles;
    }
    Set<TileCoordinates> completed = new HashSet<>(tiles);
    for (int x = minX; x <= maxX; x++) {
      for (int y = minY; y <= maxY; y++) {
        completed.add(new TileCoordinates().x(x).y(y).z(zoom));
      }
    }
    return new ArrayList<>(completed);
  }

  private List<List<List<List<BigDecimal>>>> getRestMultipolygonData(
      app.bpartners.geojobs.repository.model.Feature feature) {
    var restFeature = toRestFeature(feature);
    var jtsGeometry = featureMapper.toDomainGeometry(restFeature);

    if (jtsGeometry instanceof Polygon) {
      return List.of(restFeature.getGeometry().getPolygon().getCoordinates());
    }

    return restFeature.getGeometry().getMultiPolygon().getCoordinates();
  }

  private List<TiledPixelPolygon> getTiledPixelPolygon(
      Feature polygonGeoJsonZone,
      List<Feature> latLonRoofFeatures,
      List<DetectableType> detectableTypes,
      List<MachineDetectedTile> detectedTileList,
      boolean isParcelDetection) {
    var providedLatLonPolygonGeometry =
        geometryConverter.apply(
            List.of(polygonGeoJsonZone.getGeometry().getPolygon().getCoordinates()));

    return latLonRoofFeatures.stream()
        .map(
            roofFeature -> {
              return detectedTileList.stream()
                  .map(
                      detectedTile -> {
                        var tileCoordinates = detectedTile.getTile().getCoordinates();
                        var providedZoneInsideTileGeometry =
                            tileCoordinatesPolygonIntersection.intersection(
                                providedLatLonPolygonGeometry, tileCoordinates);
                        if (providedZoneInsideTileGeometry.isEmpty()) {
                          return null;
                        }
                        var polygonObjectTypes =
                            detectedTile.getDetectedObjects().stream()
                                .map(
                                    detectedObject -> {
                                      var detectableType =
                                          detectedObject
                                              .getDetectedObjectType()
                                              .getDetectableType();
                                      if (!detectableTypes.contains(detectableType)) {
                                        return null;
                                      }
                                      var geometryProcessed =
                                          getMultiPolygonZoneProcessed(
                                              roofFeature, isParcelDetection, detectableType);
                                      var providedZoneAndGeometryProcessedInsideTileGeometry =
                                          providedZoneInsideTileGeometry.intersection(
                                              geometryProcessed);
                                      var
                                          providedZoneAndGeometryProcessedInsideTilePolygonCoordinates =
                                              tileCoordinatesPolygonIntersection.intersects(
                                                  providedZoneAndGeometryProcessedInsideTileGeometry,
                                                  tileCoordinates);
                                      if (providedZoneAndGeometryProcessedInsideTilePolygonCoordinates
                                          .isEmpty()) {
                                        return null;
                                      }
                                      var providedZoneAndGeometryProcessedInsideTilePixelGeometry =
                                          geometryConverter.convertToPolygon(
                                              providedZoneAndGeometryProcessedInsideTilePolygonCoordinates);
                                      var polygonCoordinates =
                                          detectedObject
                                              .getFeature()
                                              .getGeometry()
                                              .getMultiPolygon()
                                              .getCoordinates()
                                              .getFirst()
                                              .getFirst();
                                      var closedPolygon =
                                          polygonCoordinatesCloser.apply(polygonCoordinates);
                                      var detectedObjectPolygonPixel =
                                          geometryConverter
                                              .toPolygon(List.of(List.of(closedPolygon)))
                                              .buffer(0);
                                      var intersectionBetweenDetectedObjectAndConsideredZone =
                                          detectedObjectPolygonPixel
                                              .intersection(
                                                  providedZoneAndGeometryProcessedInsideTilePixelGeometry)
                                              .buffer(0);
                                      if (intersectionBetweenDetectedObjectAndConsideredZone
                                          instanceof Polygon polygon) {
                                        return new PolygonObjectType(
                                            polygon, detectedObject.getDetectableObjectType());
                                      } else {
                                        log.info(
                                            "Intersection between detected object and considered"
                                                + " zone not polygon, but was {}",
                                            intersectionBetweenDetectedObjectAndConsideredZone
                                                .getGeometryType());
                                      }
                                      return null;
                                    })
                                .filter(Objects::nonNull)
                                .toList();
                        return new TiledPixelPolygon(
                            roofFeature,
                            polygonObjectTypes,
                            tileCoordinates.getX(),
                            tileCoordinates.getY(),
                            tileCoordinates.getZ());
                      })
                  .toList();
            })
        .flatMap(List::stream)
        .filter(Objects::nonNull)
        .toList();
  }
}
