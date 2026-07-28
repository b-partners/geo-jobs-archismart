package app.bpartners.geojobs.postprocessing;

import static app.bpartners.geojobs.postprocessing.BoundaryMerger.invert;
import static app.bpartners.geojobs.service.event.GeoJsonConversionTaskConsumer.NEIGHBOUR_SIZE;
import static java.util.stream.Collectors.toSet;

import app.bpartners.geojobs.model.ConversionFormatType;
import app.bpartners.geojobs.model.geometry.VGG;
import app.bpartners.geojobs.postprocessing.model.LatLonPolygon;
import app.bpartners.geojobs.postprocessing.model.TiledPolygon;
import app.bpartners.geojobs.postprocessing.model.TilingConf;
import app.bpartners.geojobs.repository.model.detection.DetectableType;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class DetectionBoundaryMerger {

  public Set<LatLonPolygon> apply(
      Set<TiledPolygon> tiledPolygons, ConversionFormatType conversionFormatType) {
    if (tiledPolygons.isEmpty()) {
      return Set.of();
    }
    var toProcess = handleTiledPolygonsByConversionFormat(tiledPolygons, conversionFormatType);
    return toProcess.stream()
        .collect(Collectors.groupingBy(this::getDetectableType))
        .entrySet()
        .stream()
        .flatMap(
            entry -> {
              var detectableType = entry.getKey();
              var merger =
                  new BoundaryMerger(detectableType.getMinAreaThreshold(), NEIGHBOUR_SIZE, true);
              return merger.apply(Set.copyOf(entry.getValue()), detectableType).stream();
            })
        .collect(Collectors.toSet());
  }

  private Set<TiledPolygon> handleTiledPolygonsByConversionFormat(
      Set<TiledPolygon> tiledPolygons, ConversionFormatType conversionFormatType) {
    switch (conversionFormatType) {
      case GEO_JSON -> {
        var latLonPolygons =
            tiledPolygons.stream().map(TiledPolygon::latLonPolygon).collect(toSet());
        var inverted = invert(latLonPolygons);
        return toTiledPolygon(inverted, tiledPolygons.iterator().next().tilingConf());
      }
      case VGG -> {
        return tiledPolygons;
      }
      default ->
          throw new IllegalArgumentException(
              "Unknown conversion format type: " + conversionFormatType);
    }
  }

  public Set<TiledPolygon> applyVgg(VGG vgg) {
    var tilingConf = TilingConf.getDefaultInstance();
    var toUnify = toTiledPolygons(tilingConf, vgg);
    if (toUnify.isEmpty()) return Set.of();
    var originTile = toUnify.iterator().next().originTile();
    return apply(toUnify, ConversionFormatType.VGG).stream()
        .map(latLon -> latLon.tiledPolygon(tilingConf, originTile))
        .collect(toSet());
  }

  private Set<TiledPolygon> toTiledPolygons(TilingConf tilingConf, VGG vgg) {
    return TiledPolygon.toTiledPolygons(tilingConf, vgg, false);
  }

  private Set<TiledPolygon> toTiledPolygon(Set<LatLonPolygon> polygons, TilingConf tilingConf) {
    return polygons.stream().map(latLon -> latLon.tiledPolygon(tilingConf)).collect(toSet());
  }

  private DetectableType getDetectableType(TiledPolygon tp) {
    var userData = (Map<String, Object>) tp.polygon().getUserData();
    if (userData != null && userData.containsKey("label")) {
      var label = userData.get("label").toString().toUpperCase();
      try {
        return DetectableType.valueOf(label);
      } catch (IllegalArgumentException e) {
        log.warn("Unknown detectable type from label: {}", label);
      }
    }
    try {
      return DetectableType.valueOf(tp.type().name().toUpperCase());
    } catch (IllegalArgumentException e) {
      log.warn("Unknown detectable type from ObjectType: {}", tp.type());
      return DetectableType.BACKGROUND;
    }
  }
}
