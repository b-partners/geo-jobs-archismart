package app.bpartners.geojobs.service.geojson;

import static app.bpartners.geojobs.endpoint.rest.postprocessing.BoundaryMerger.invert;
import static app.bpartners.geojobs.repository.model.detection.DetectableType.*;
import static app.bpartners.geojobs.service.geojson.GeoJson.fromFeatures;

import app.bpartners.geojobs.endpoint.rest.postprocessing.DetectionBoundaryMerger;
import app.bpartners.geojobs.endpoint.rest.postprocessing.model.LatLonPolygon;
import app.bpartners.geojobs.endpoint.rest.postprocessing.model.TilingConf;
import app.bpartners.geojobs.model.ConversionFormatType;
import app.bpartners.geojobs.model.DetectedTile;
import app.bpartners.geojobs.repository.model.detection.DetectedObject;
import java.util.List;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@AllArgsConstructor
public class GeoJsonConverter implements Converter<List<DetectedTile>, GeoJson> {
  private static final int DEFAULT_IMAGE_SIZE = 1024;
  private final GeoJsonMapper mapper;
  private final DetectionBoundaryMerger merger;

  @Override
  public GeoJson convert(List<DetectedTile> detectedTiles) {
    var detectableTypes =
        detectedTiles.stream()
            .flatMap(tile -> tile.getDetectedObjects().stream())
            .map(DetectedObject::getDetectableObjectType)
            .collect(Collectors.toSet());
    List<GeoJson.GeoFeature> geoFeatures =
        detectedTiles.stream()
            .map(
                detectedTile -> {
                  var tile = detectedTile.getTile();
                  var xTile = tile.getCoordinates().getX();
                  var yTile = tile.getCoordinates().getY();
                  var zoom = tile.getCoordinates().getZ();
                  return mapper.toGeoFeatures(
                      xTile, yTile, zoom, DEFAULT_IMAGE_SIZE, detectedTile.getDetectedObjects());
                })
            .flatMap(List::stream)
            .toList();
    log.info("DEBUG Detectable types for converting geojson : {}", detectableTypes);
    if (detectableTypes.size() == 1
        && (detectableTypes.contains(OBSTACLE)
            || detectableTypes.contains(CHEMINEE)
            || detectableTypes.contains(VELUX))) {
      return fromFeatures(geoFeatures);
    }

    var toUnify =
        geoFeatures.stream()
            .map(f -> LatLonPolygon.latLon(f).tiledPolygon(TilingConf.getDefaultInstance()))
            .collect(Collectors.toSet());

    var unifiedLatLon = merger.apply(toUnify, ConversionFormatType.GEO_JSON);
    var invertedUnifiedLatLon = invert(unifiedLatLon);
    var unifiedGeoFeatures =
        invertedUnifiedLatLon.stream().map(LatLonPolygon::toGeoFeature).toList();

    return fromFeatures(unifiedGeoFeatures);
  }
}
