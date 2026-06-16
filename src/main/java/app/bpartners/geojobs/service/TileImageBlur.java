package app.bpartners.geojobs.service;

import static app.bpartners.geojobs.endpoint.rest.controller.mapper.FeatureMapper.toRestFeature;
import static app.bpartners.geojobs.endpoint.rest.model.DelimitationType.ROOF;
import static app.bpartners.geojobs.endpoint.rest.model.DelimitationType.USER_DEFINED_DELIMITATION;
import static app.bpartners.geojobs.service.geojson.GeometryConverter.unifyMultiPolygon;
import static java.awt.Color.WHITE;
import static org.locationtech.jts.geom.util.GeometryCombiner.combine;

import app.bpartners.geojobs.endpoint.rest.model.MultiPolygon;
import app.bpartners.geojobs.endpoint.rest.model.Polygon;
import app.bpartners.geojobs.endpoint.rest.model.TileCoordinates;
import app.bpartners.geojobs.model.geometry.IntXY;
import app.bpartners.geojobs.repository.model.detection.Detection;
import app.bpartners.geojobs.repository.model.tiling.Tile;
import app.bpartners.geojobs.service.geojson.GeometryConverter;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryCollection;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class TileImageBlur implements BiFunction<Detection, List<Tile>, List<Tile>> {
  private static final int DEFAULT_TILE_SIZE = 1024;
  private final GeometryPixelProjector geometryPixelProjector;
  private final GeometryConverter geometryConverter;
  private final FilePolygonDrawer filePolygonDrawer;
  private final DetectionBackgroundRetriever detectionBackgroundRetriever;
  private final DetectionZoneToProcessProvider detectionProvidedZoneUnifier;

  public List<Tile> apply(Geometry polygonGeometry, List<Tile> tiles) {
    return tiles.stream()
        .map(
            tile -> {
              var tileCoordinates = tile.getCoordinates();
              var multiPolygonFromTile =
                  geometryConverter.getMultiPolygonFromTile(
                      tileCoordinates.getX(), tileCoordinates.getY(), tileCoordinates.getZ());
              List<List<List<IntXY>>> multiPolygonPixelCoordinates;
              if (!multiPolygonFromTile.intersects(polygonGeometry)) {
                multiPolygonPixelCoordinates = getBlurAllAreaCoordinates();
              } else {
                var roofInsideTile =
                    handleGeometryCollectionType(
                        multiPolygonFromTile.intersection(polygonGeometry));
                var tileWithoutRoof = multiPolygonFromTile.difference(roofInsideTile);
                multiPolygonPixelCoordinates =
                    retrievePixelBackgroundCoordinates(tileCoordinates, tileWithoutRoof);
              }
              var imageWithBlur =
                  filePolygonDrawer.apply(multiPolygonPixelCoordinates, WHITE, tile.getImage());
              if (imageWithBlur == null) {
                log.info(
                    "Unable to blur image for tile x: {} / y: {}",
                    tileCoordinates.getX(),
                    tileCoordinates.getY());
              }
              return tile.toBuilder().image(imageWithBlur).build();
            })
        .toList();
  }

  private List<List<List<IntXY>>> retrievePixelBackgroundCoordinates(
      TileCoordinates tileCoordinates, Geometry tileWithoutRoof) {
    List<List<List<IntXY>>> multiPolygonPixelCoordinates;
    var backgroundMultiPolygonPixels =
        geometryPixelProjector.toMultiPolygonPixels(
            tileWithoutRoof,
            tileCoordinates.getX(),
            tileCoordinates.getY(),
            tileCoordinates.getZ(),
            DEFAULT_TILE_SIZE);
    multiPolygonPixelCoordinates =
        backgroundMultiPolygonPixels.stream()
            .map(
                polygon ->
                    polygon.stream()
                        .map(
                            ring ->
                                ring.stream()
                                    .map(
                                        coordinates ->
                                            new IntXY(
                                                coordinates.getFirst().intValue(),
                                                coordinates.getLast().intValue()))
                                    .toList())
                        .toList())
            .toList();
    return multiPolygonPixelCoordinates;
  }

  @Override
  public List<Tile> apply(Detection detection, List<Tile> tiles) {
    var latLonBackgroundInsideProvidedZone = detectionBackgroundRetriever.apply(detection);
    var providedZone = detectionProvidedZoneUnifier.apply(detection);
    var geoJsonDelimitationType = detection.getGeoJsonDelimitationType();
    Geometry zoneToExcludeInsideProvidedZone;
    switch (geoJsonDelimitationType) {
      case ROOF, USER_DEFINED_DELIMITATION -> zoneToExcludeInsideProvidedZone = providedZone;
      case ZONE, PARCEL_FREE_DELIMITATION -> {
        var unifiedRoofMultiPolygon = getUnifiedRoofMultiPolygon(detection);
        zoneToExcludeInsideProvidedZone =
            handleGeometryCollectionType(providedZone.intersection(unifiedRoofMultiPolygon));
      }
      case PARCEL, PARCEL_CONSTRAINED_DELIMITATION -> {
        var unifiedParcelsMultiPolygon = getUnifiedParcelsMultiPolygon(detection);
        zoneToExcludeInsideProvidedZone =
            handleGeometryCollectionType(providedZone.intersection(unifiedParcelsMultiPolygon));
      }
      default ->
          throw new IllegalArgumentException(
              "Unsupported geoJsonDelimitationType: " + geoJsonDelimitationType);
    }
    var bluredTiles =
        tiles.stream()
            .map(
                tile -> {
                  boolean isBlured = false;
                  var tileCoordinates = tile.getCoordinates();
                  var multiPolygonFromTile =
                      geometryConverter.getMultiPolygonFromTile(
                          tileCoordinates.getX(), tileCoordinates.getY(), tileCoordinates.getZ());
                  var excludedZoneInsideTileAndProvidedZone =
                      handleGeometryCollectionType(
                          multiPolygonFromTile.intersection(zoneToExcludeInsideProvidedZone));

                  Geometry intersectionBetweenTileMultiPolygonAndBackground;
                  if (ROOF.equals(geoJsonDelimitationType)
                      || USER_DEFINED_DELIMITATION.equals(geoJsonDelimitationType)) {
                    intersectionBetweenTileMultiPolygonAndBackground =
                        multiPolygonFromTile.difference(excludedZoneInsideTileAndProvidedZone);
                  } else {
                    intersectionBetweenTileMultiPolygonAndBackground =
                        handleGeometryCollectionType(
                            multiPolygonFromTile.intersection(latLonBackgroundInsideProvidedZone));
                  }

                  var tileWithoutRoofInsideTileAndZone =
                      multiPolygonFromTile.difference(excludedZoneInsideTileAndProvidedZone);
                  List<List<List<IntXY>>> multiPolygonPixelCoordinates;
                  if (intersectionBetweenTileMultiPolygonAndBackground == null
                      || intersectionBetweenTileMultiPolygonAndBackground.isEmpty()) {
                    multiPolygonPixelCoordinates = getBlurAllAreaCoordinates();
                    isBlured = true;
                  } else {
                    multiPolygonPixelCoordinates =
                        retrievePixelBackgroundCoordinates(
                            tileCoordinates, tileWithoutRoofInsideTileAndZone);
                  }
                  var imageWithBlur =
                      filePolygonDrawer.apply(multiPolygonPixelCoordinates, WHITE, tile.getImage());
                  if (imageWithBlur == null) {
                    log.info(
                        "Unable to blur image for detection (e2Id= {}) - tile x: {} / y: {}",
                        detection.getEndToEndId(),
                        tileCoordinates.getX(),
                        tileCoordinates.getY());
                  }
                  return tile.toBuilder().image(imageWithBlur).isBlured(isBlured).build();
                })
            .toList();
    if (bluredTiles.stream().allMatch(Tile::isBlured)) {
      log.info(
          "Returning original images as error occurred during blurring images for tiles {}",
          tiles.stream()
                  .map(
                      tile ->
                          "x:"
                              + tile.getCoordinates().getX()
                              + "/ y: "
                              + tile.getCoordinates().getY())
                  .toList()
              + " ; ");
      return tiles;
    }
    return bluredTiles;
  }

  private org.locationtech.jts.geom.MultiPolygon getUnifiedRoofMultiPolygon(Detection detection) {
    return detection.getFeatureWithDelimitations().stream()
        .map(
            featureWithDelimitation ->
                featureWithDelimitation.delimitations().stream()
                    .map(
                        f -> {
                          var geometryType = toRestFeature(f).getGeometry().getActualInstance();
                          switch (geometryType) {
                            case Polygon polygon -> {
                              return geometryConverter.apply(List.of(polygon.getCoordinates()));
                            }
                            case MultiPolygon multiPolygon -> {
                              return geometryConverter.apply(multiPolygon.getCoordinates());
                            }
                            default ->
                                throw new IllegalArgumentException(
                                    "Unsupported geometry type to extended image: " + geometryType);
                          }
                        })
                    .toList())
        .toList()
        .stream()
        .flatMap(List::stream)
        .reduce(unifyMultiPolygon())
        .orElseThrow(
            () ->
                new IllegalStateException(
                    "Unable to unify delimitation multiPolygon for detection.id: "
                        + detection.getId()));
  }

  private org.locationtech.jts.geom.MultiPolygon getUnifiedParcelsMultiPolygon(
      Detection detection) {
    return detection.getParcelDelimitations().stream()
        .map(
            featureWithDelimitation ->
                featureWithDelimitation.delimitations().stream()
                    .map(
                        f -> {
                          var geometryType = toRestFeature(f).getGeometry().getActualInstance();
                          switch (geometryType) {
                            case Polygon polygon -> {
                              return geometryConverter.apply(List.of(polygon.getCoordinates()));
                            }
                            case MultiPolygon multiPolygon -> {
                              return geometryConverter.apply(multiPolygon.getCoordinates());
                            }
                            default ->
                                throw new IllegalArgumentException(
                                    "Unsupported geometry type to extended image: " + geometryType);
                          }
                        })
                    .toList())
        .toList()
        .stream()
        .flatMap(List::stream)
        .reduce(unifyMultiPolygon())
        .orElseThrow(
            () ->
                new IllegalStateException(
                    "Unable to unify delimitation multiPolygon for detection.id: "
                        + detection.getId()));
  }

  private List<List<List<IntXY>>> getBlurAllAreaCoordinates() {
    return List.of(
        List.of(
            List.of(
                new IntXY(0, 0),
                new IntXY(0, DEFAULT_TILE_SIZE),
                new IntXY(DEFAULT_TILE_SIZE, DEFAULT_TILE_SIZE),
                new IntXY(DEFAULT_TILE_SIZE, 0))));
  }

  private Geometry handleGeometryCollectionType(Geometry geometry) {
    if (geometry instanceof GeometryCollection collection) {
      List<Geometry> geometries = new ArrayList<>();
      for (int i = 0; i < collection.getNumGeometries(); i++) {
        var geometryN = collection.getGeometryN(i);
        if (geometryN instanceof org.locationtech.jts.geom.Polygon
            || geometryN instanceof org.locationtech.jts.geom.MultiPolygon) {
          geometries.add(geometryN);
        } else {
          log.info(
              "TileImageBlur: Unsupported geometry {} in geometry collection", geometryN.toText());
        }
      }
      return combine(geometries);
    }
    return geometry;
  }
}
