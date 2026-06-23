package app.bpartners.geojobs.service;

import static app.bpartners.geojobs.service.geojson.GeometryConverter.getMultiPolygonZoneProcessed;

import app.bpartners.geojobs.endpoint.rest.model.Feature;
import app.bpartners.geojobs.model.geometry.PolygonObjectType;
import app.bpartners.geojobs.model.geometry.TiledPixelPolygon;
import app.bpartners.geojobs.repository.model.detection.DetectableType;
import app.bpartners.geojobs.repository.model.detection.MachineDetectedTile;
import app.bpartners.geojobs.service.geojson.GeometryConverter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.geom.util.GeometryFixer;
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
        GeometryFixer.fix(
            geometryConverter.apply(
                List.of(polygonGeoJsonZone.getGeometry().getPolygon().getCoordinates())));

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
                                  .flatMap(
                                      detectedObject -> {
                                        var detectableType =
                                            detectedObject
                                                .getDetectedObjectType()
                                                .getDetectableType();
                                        if (!detectableTypes.contains(detectableType)) {
                                          return Stream.<PolygonObjectType>empty();
                                        }
                                        var geometryProcessed =
                                            getMultiPolygonZoneProcessed(
                                                delimitationFeature,
                                                isParcelDetection,
                                                detectableType);
                                        if (geometryProcessed == null) {
                                          return Stream.empty();
                                        }
                                        var providedZoneAndGeometryProcessedInsideTileGeometry =
                                            providedZoneInsideTileGeometry.intersection(
                                                GeometryFixer.fix(geometryProcessed));
                                        var
                                            providedZoneAndGeometryProcessedInsideTilePixelGeometry =
                                                tileCoordinatesPolygonIntersection
                                                    .intersectsAsPixelGeometry(
                                                        providedZoneAndGeometryProcessedInsideTileGeometry,
                                                        tileCoordinates);
                                        if (providedZoneAndGeometryProcessedInsideTilePixelGeometry
                                            .isEmpty()) {
                                          return Stream.<PolygonObjectType>empty();
                                        }
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
                                        var objectType = detectedObject.getDetectableObjectType();
                                        var polygonObjectTypes = new ArrayList<PolygonObjectType>();
                                        for (int g = 0;
                                            g
                                                < intersectionBetweenDetectedObjectAndConsideredZone
                                                    .getNumGeometries();
                                            g++) {
                                          if (intersectionBetweenDetectedObjectAndConsideredZone
                                                      .getGeometryN(g)
                                                  instanceof Polygon polygon
                                              && !polygon.isEmpty()) {
                                            polygonObjectTypes.add(
                                                new PolygonObjectType(polygon, objectType));
                                          }
                                        }
                                        if (polygonObjectTypes.isEmpty()) {
                                          log.info(
                                              "Intersection between detected object and considered"
                                                  + " zone has no polygonal part, but was {}",
                                              intersectionBetweenDetectedObjectAndConsideredZone
                                                  .getGeometryType());
                                        }
                                        return polygonObjectTypes.stream();
                                      })
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
