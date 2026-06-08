package app.bpartners.geojobs.service;

import static app.bpartners.geojobs.endpoint.rest.model.Detection.GeoJsonDelimitationTypeEnum.PARCEL;

import app.bpartners.geojobs.endpoint.rest.model.Detection;
import app.bpartners.geojobs.endpoint.rest.model.Feature;
import app.bpartners.geojobs.endpoint.rest.model.Point;
import app.bpartners.geojobs.service.geojson.GeometryConverter;
import app.bpartners.geojobs.service.ign.IgnCadastreFeatureFetcher;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.function.BiFunction;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.Polygon;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class DetectionPolygonProcessedRetriever
    implements BiFunction<Feature, Detection.GeoJsonDelimitationTypeEnum, Polygon> {
  private final IgnCadastreFeatureFetcher ignCadastreFeatureFetcher;
  private final GeometryConverter geometryConverter;
  private final BuildingFinder buildingFinder;

  @Override
  public Polygon apply(
      Feature feature, Detection.GeoJsonDelimitationTypeEnum delimitationTypeEnum) {
    var geometryInstance = feature.getGeometry().getActualInstance();
    switch (geometryInstance) {
      case app.bpartners.geojobs.endpoint.rest.model.Point point -> {
        return retrievePolygonZoneGeomFromPoint(delimitationTypeEnum, point);
      }
      case app.bpartners.geojobs.endpoint.rest.model.Polygon restPolygon -> {
        return geometryConverter.convertToPolygon(restPolygon.getCoordinates().getFirst());
      }
      case app.bpartners.geojobs.endpoint.rest.model.MultiPolygon restMultiPolygon -> {
        var jtsMultiPolygon = geometryConverter.apply(restMultiPolygon.getCoordinates());
        if (jtsMultiPolygon.getNumGeometries() > 1) {
          log.error("Unable to handle multiPolygons for feature : {}", feature);
          return null;
        } else {
          return (Polygon) jtsMultiPolygon.getGeometryN(0);
        }
      }
      default -> {
        log.error("Unable to handle geometry type : {}", geometryInstance);
        return null;
      }
    }
  }

  @SneakyThrows
  public Polygon retrievePolygonZoneGeomFromPoint(
      Detection.GeoJsonDelimitationTypeEnum delimitationTypeEnum, Point point) {
    MultiPolygon zoneToRetrievePolygon;
    if (PARCEL.equals(delimitationTypeEnum)) {
      var parcelFeaturesFromPoint =
          ignCadastreFeatureFetcher.apply(
              geometryConverter.readGeometryFromString(
                  new ObjectMapper().writeValueAsString(point)));
      if (parcelFeaturesFromPoint.isEmpty()) {
        log.warn("No parcel found for point {}", point);
        return null;
      }
      zoneToRetrievePolygon =
          geometryConverter.retrieveNearestParcelMultiPolygon(parcelFeaturesFromPoint);
    } else {
      zoneToRetrievePolygon = buildingFinder.getBuildingMultiPolygon(point);
    }
    if (zoneToRetrievePolygon.getNumGeometries() > 1) {
      log.error("Unable to retrieve polygon zone processed for : {}", point.toString());
      return null;
    } else {
      return (Polygon) zoneToRetrievePolygon.getGeometryN(0);
    }
  }
}
