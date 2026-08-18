package app.bpartners.geojobs.service.geojson;

import static app.bpartners.geojobs.model.geometry.GeometryFactory.geometryFactory;
import static app.bpartners.geojobs.postprocessing.BoundaryMerger.invert;
import static app.bpartners.geojobs.repository.model.detection.DetectableType.*;
import static app.bpartners.geojobs.service.geojson.GeoJson.fromFeatures;

import app.bpartners.geojobs.model.ConversionFormatType;
import app.bpartners.geojobs.model.DetectedTile;
import app.bpartners.geojobs.postprocessing.DetectionBoundaryMerger;
import app.bpartners.geojobs.postprocessing.model.LatLonPolygon;
import app.bpartners.geojobs.postprocessing.model.TilingConf;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.geom.TopologyException;
import org.locationtech.jts.geom.util.GeometryFixer;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@AllArgsConstructor
public class GeoJsonConverter implements BiFunction<List<DetectedTile>, MultiPolygon, GeoJson> {
  private static final int DEFAULT_IMAGE_SIZE = 1024;
  private final GeoJsonMapper mapper;
  private final DetectionBoundaryMerger merger;
  private final GeometryConverter geometryConverter;

  @Override
  public GeoJson apply(
      List<DetectedTile> detectedTiles, MultiPolygon providedGeometryMultiPolygon) {
    List<GeoJson.GeoFeature> geoFeatures =
        new ArrayList<>(
            detectedTiles.stream()
                .map(
                    detectedTile -> {
                      var tile = detectedTile.getTile();
                      var tileCoordinates = tile.getCoordinates();
                      var xTile = tileCoordinates.getX();
                      var yTile = tileCoordinates.getY();
                      var zoom = tileCoordinates.getZ();
                      return mapper.toGeoFeatures(
                          xTile,
                          yTile,
                          zoom,
                          DEFAULT_IMAGE_SIZE,
                          detectedTile.getDetectedObjects());
                    })
                .flatMap(List::stream)
                .toList());

    // Parking features are extracted (and removed) from geoFeatures before clipping, so they are
    // not duplicated between convertedGeoFeatures and the unified parking features below.
    var unifiedStationnementObjects = unifyStationnementModelObjects(geoFeatures);

    List<GeoJson.GeoFeature> convertedGeoFeatures =
        new ArrayList<>(clipAllToProvidedGeometry(geoFeatures, providedGeometryMultiPolygon));

    convertedGeoFeatures.addAll(
        clipAllToProvidedGeometry(unifiedStationnementObjects, providedGeometryMultiPolygon));

    return fromFeatures(convertedGeoFeatures);
  }

  private List<GeoJson.GeoFeature> clipAllToProvidedGeometry(
      List<GeoJson.GeoFeature> geoFeatures, MultiPolygon providedGeometryMultiPolygon) {
    if (providedGeometryMultiPolygon == null) {
      return geoFeatures;
    }
    return geoFeatures.stream()
        .map(geoFeature -> clipToProvidedGeometry(geoFeature, providedGeometryMultiPolygon))
        .filter(Objects::nonNull)
        .toList();
  }

  private GeoJson.GeoFeature clipToProvidedGeometry(
      GeoJson.GeoFeature geoFeature, MultiPolygon providedGeometryMultiPolygon) {
    if (geoFeature.getGeometry() == null || geoFeature.getGeometry().getCoordinates() == null) {
      return null;
    }
    var multiPolygon = geometryConverter.apply(geoFeature.getGeometry().getCoordinates());
    var detectedGeoFeatureInsideProvidedGeometry =
        intersection(providedGeometryMultiPolygon, multiPolygon);
    if (detectedGeoFeatureInsideProvidedGeometry.isEmpty()) {
      return null;
    }
    if (detectedGeoFeatureInsideProvidedGeometry
        instanceof MultiPolygon multiPolygonInsideProvidedGeometry) {
      geoFeature.setGeometry(
          geometryConverter.restMultiPolygonFromJts(multiPolygonInsideProvidedGeometry));
      return geoFeature;
    }
    if (detectedGeoFeatureInsideProvidedGeometry instanceof Polygon polygonInsideProvidedGeometry) {
      var multiPolygonFromPolygon =
          geometryFactory.createMultiPolygon(new Polygon[] {polygonInsideProvidedGeometry});
      geoFeature.setGeometry(geometryConverter.restMultiPolygonFromJts(multiPolygonFromPolygon));
      return geoFeature;
    }
    var polygonal = extractPolygonal(detectedGeoFeatureInsideProvidedGeometry);
    if (polygonal != null) {
      geoFeature.setGeometry(geometryConverter.restMultiPolygonFromJts(polygonal));
      return geoFeature;
    }
    log.error(
        "Unable to handle geometry intersection type {} provided geometry with"
            + " geoFeature.geometry : {}",
        detectedGeoFeatureInsideProvidedGeometry.getClass().getSimpleName(),
        geoFeature.getGeometry());
    return null;
  }

  private static MultiPolygon extractPolygonal(Geometry geometry) {
    var polygons = new java.util.ArrayList<Polygon>();
    for (int i = 0; i < geometry.getNumGeometries(); i++) {
      if (geometry.getGeometryN(i) instanceof Polygon polygon && !polygon.isEmpty()) {
        polygons.add(polygon);
      }
    }
    if (polygons.isEmpty()) {
      return null;
    }
    return geometryFactory.createMultiPolygon(polygons.toArray(new Polygon[0]));
  }

  private static Geometry intersection(Geometry a, Geometry b) {
    try {
      return a.intersection(b);
    } catch (TopologyException e) {
      log.warn(
          "TopologyException during intersection, retrying with fixed geometries: {}",
          e.getMessage());
      return GeometryFixer.fix(a).intersection(GeometryFixer.fix(b));
    }
  }

  @NotNull
  private List<GeoJson.GeoFeature> unifyStationnementModelObjects(
      List<GeoJson.GeoFeature> geoFeatures) {
    List<GeoJson.GeoFeature> stationnementModelObjectFeatures = new ArrayList<>();
    geoFeatures.removeIf(
        f -> {
          if (f.getProperties() != null
              && f.getProperties().containsKey("label")
              && geoFeatureContainsStationnementModelObjects(f)) {
            stationnementModelObjectFeatures.add(f);
            return true;
          }
          return false;
        });

    return unifyGeoFeatures(stationnementModelObjectFeatures);
  }

  private boolean geoFeatureContainsStationnementModelObjects(GeoJson.GeoFeature f) {
    var parkingLabel = PARKING.name().toLowerCase();
    var geoFeatureLabel = f.getProperties().get("label").toString().toLowerCase();
    return parkingLabel.equals(geoFeatureLabel);
  }

  @NotNull
  private List<GeoJson.GeoFeature> unifyGeoFeatures(List<GeoJson.GeoFeature> geoFeatures) {
    var toUnify =
        geoFeatures.stream()
            .map(f -> LatLonPolygon.latLon(f).tiledPolygon(TilingConf.getDefaultInstance()))
            .collect(Collectors.toSet());

    var unifiedLatLon = merger.apply(toUnify, ConversionFormatType.GEO_JSON);
    var invertedUnifiedLatLon = invert(unifiedLatLon);
    return invertedUnifiedLatLon.stream().map(LatLonPolygon::toGeoFeature).toList();
  }
}
