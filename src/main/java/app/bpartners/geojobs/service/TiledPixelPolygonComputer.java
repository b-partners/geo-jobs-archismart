package app.bpartners.geojobs.service;

import static app.bpartners.geojobs.service.geojson.GeometryConverter.getMultiPolygonZoneProcessed;

import app.bpartners.geojobs.endpoint.rest.model.Feature;
import app.bpartners.geojobs.model.geometry.PolygonObjectType;
import app.bpartners.geojobs.model.geometry.TiledPixelPolygon;
import app.bpartners.geojobs.repository.model.detection.DetectableType;
import app.bpartners.geojobs.repository.model.detection.MachineDetectedTile;
import app.bpartners.geojobs.service.geojson.GeometryConverter;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.locationtech.jts.geom.Polygon;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class TiledPixelPolygonComputer {
  private final GeometryConverter geometryConverter;
  private final TileCoordinatesPolygonIntersection tileCoordinatesPolygonIntersection;
  private final PolygonCoordinatesCloser polygonCoordinatesCloser;

  public List<TiledPixelPolygon> apply(
      Feature polygonGeoJsonZone,
      List<Feature> delimitationFeatures,
      List<DetectableType> detectableTypes,
      List<MachineDetectedTile> detectedTileList,
      boolean isParcelDetection) {
    var providedLatLonPolygonGeometry =
        geometryConverter.apply(
            List.of(polygonGeoJsonZone.getGeometry().getPolygon().getCoordinates()));

    return delimitationFeatures.stream()
        .map(
            delimitationFeature ->
                detectedTileList.stream()
                    .map(
                        detectedTile -> {
                          var tileCoordinates = detectedTile.getTile().getCoordinates();
                          var providedZoneInsideTileGeometry =
                              tileCoordinatesPolygonIntersection.intersection(
                                  providedLatLonPolygonGeometry, tileCoordinates);
                          if (providedZoneInsideTileGeometry.isEmpty()) {
                            return null;
                          }
                          var detectedPolygonObjectTypes =
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
                                                delimitationFeature,
                                                isParcelDetection,
                                                detectableType);
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
                                        var
                                            providedZoneAndGeometryProcessedInsideTilePixelGeometry =
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
                              delimitationFeature,
                              detectedPolygonObjectTypes,
                              tileCoordinates.getX(),
                              tileCoordinates.getY(),
                              tileCoordinates.getZ());
                        })
                    .toList())
        .flatMap(List::stream)
        .filter(Objects::nonNull)
        .toList();
  }
}
