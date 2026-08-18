package app.bpartners.geojobs.postprocessing;

import static app.bpartners.geojobs.repository.model.detection.DetectableType.PARKING;
import static app.bpartners.geojobs.repository.model.detection.DetectableType.PLACE_STANDARD;
import static app.bpartners.geojobs.service.geojson.GeoJson.fromFeatures;

import app.bpartners.geojobs.endpoint.rest.model.MultiPolygon;
import app.bpartners.geojobs.repository.model.detection.DetectableType;
import app.bpartners.geojobs.repository.model.detection.Detection;
import app.bpartners.geojobs.service.GeometrySquareMeterArea;
import app.bpartners.geojobs.service.geojson.GeoJson;
import app.bpartners.geojobs.service.geojson.GeometryConverter;
import java.util.ArrayList;
import java.util.Set;
import java.util.function.BiFunction;
import javax.annotation.Nullable;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Polygonal;
import org.locationtech.jts.geom.util.GeometryFixer;
import org.springframework.stereotype.Component;

/**
 * Post-processes the objects detected by the STATIONNEMENT model only: the model returns noisy
 * PLACE_STANDARD objects that are far too small to be an actual parking place, and those objects
 * must not be delivered. Objects detected by any other model are left untouched.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StationnementPostProcessor implements BiFunction<GeoJson, Detection, GeoJson> {
  public static final double MIN_PLACE_STANDARD_AREA_IN_SQUARE_METER = 9.0;
  public static final String AREA_IN_SQUARE_METER_PROPERTY = "area_in_m_2";
  public static final Set<DetectableType> AREA_DELIVERED_TYPES = Set.of(PLACE_STANDARD, PARKING);

  private final GeometryConverter geometryConverter;
  private final GeometrySquareMeterArea geometrySquareMeterArea;

  @Override
  public GeoJson apply(GeoJson geoJson, @Nullable Detection detection) {
    if (geoJson == null
        || geoJson.getGeoFeatures() == null
        || detection == null
        || !detection.hasStationnementModelName()) {
      return geoJson;
    }
    var geoFeatures = geoJson.getGeoFeatures();
    var withoutNoise = new ArrayList<GeoJson.GeoFeature>();
    var withDeliveredArea = 0;
    var repairedCount = 0;
    for (var geoFeature : geoFeatures) {
      if (!hasAreaDelivered(geoFeature)) {
        withoutNoise.add(geoFeature);
        continue;
      }
      withDeliveredArea++;
      var processedGeometry = repairGeometryAndComputeArea(geoFeature);
      if (processedGeometry.repaired()) {
        repairedCount++;
      }
      var areaInSquareMeter = processedGeometry.areaInSquareMeter();
      geoFeature.getProperties().put(AREA_IN_SQUARE_METER_PROPERTY, areaInSquareMeter);
      if (!isPlaceStandard(geoFeature) || !isNoise(areaInSquareMeter)) {
        withoutNoise.add(geoFeature);
      }
    }
    if (withDeliveredArea == 0) {
      return geoJson;
    }
    if (repairedCount > 0) {
      log.info(
          "{} invalid geometry(ies) repaired for detection(id={})",
          repairedCount,
          detection.getId());
    }
    if (withoutNoise.size() < geoFeatures.size()) {
      log.info(
          "{} noisy {} object(s) filtered out for detection(id={})",
          geoFeatures.size() - withoutNoise.size(),
          PLACE_STANDARD,
          detection.getId());
    }
    return fromFeatures(withoutNoise);
  }

  private boolean isNoise(@Nullable Double areaInSquareMeter) {
    return areaInSquareMeter != null && areaInSquareMeter < MIN_PLACE_STANDARD_AREA_IN_SQUARE_METER;
  }

  private boolean hasAreaDelivered(GeoJson.GeoFeature geoFeature) {
    return AREA_DELIVERED_TYPES.stream().anyMatch(type -> hasLabel(geoFeature, type));
  }

  private boolean isPlaceStandard(GeoJson.GeoFeature geoFeature) {
    return hasLabel(geoFeature, PLACE_STANDARD);
  }

  private boolean hasLabel(GeoJson.GeoFeature geoFeature, DetectableType detectableType) {
    var properties = geoFeature.getProperties();
    if (properties == null) {
      return false;
    }
    var label = properties.get("label");
    return label != null && detectableType.name().equalsIgnoreCase(label.toString());
  }

  private ProcessedGeometry repairGeometryAndComputeArea(GeoJson.GeoFeature geoFeature) {
    var geometry = geoFeature.getGeometry();
    if (geometry == null || geometry.getCoordinates() == null) {
      return ProcessedGeometry.NOT_COMPUTED;
    }
    try {
      var jtsGeometry = geometryConverter.apply(geometry.getCoordinates());
      var repaired = jtsGeometry.isValid() ? null : repair(jtsGeometry, geometry, geoFeature);
      return new ProcessedGeometry(
          geometrySquareMeterArea.apply(repaired == null ? jtsGeometry : repaired),
          repaired != null);
    } catch (RuntimeException e) {
      log.warn(
          "Unable to compute area of detected {} object, it is delivered without area and not"
              + " filtered out: {}",
          geoFeature.getProperties().get("label"),
          e.getMessage());
      return ProcessedGeometry.NOT_COMPUTED;
    }
  }

  @Nullable
  private Geometry repair(
      Geometry invalidGeometry, MultiPolygon deliveredGeometry, GeoJson.GeoFeature geoFeature) {
    var repaired = GeometryFixer.fix(invalidGeometry);
    if (repaired.isEmpty() || !(repaired instanceof Polygonal)) {
      log.warn(
          "Unable to repair invalid geometry of detected {} object, it is delivered as detected",
          geoFeature.getProperties().get("label"));
      return null;
    }
    deliveredGeometry.setCoordinates(geometryConverter.geometryToMultiPolygonCoordinates(repaired));
    return repaired;
  }

  private record ProcessedGeometry(@Nullable Double areaInSquareMeter, boolean repaired) {
    private static final ProcessedGeometry NOT_COMPUTED = new ProcessedGeometry(null, false);
  }
}
