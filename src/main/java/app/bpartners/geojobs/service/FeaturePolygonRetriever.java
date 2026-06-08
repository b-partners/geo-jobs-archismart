package app.bpartners.geojobs.service;

import static app.bpartners.geojobs.endpoint.rest.controller.mapper.FeatureMapper.toRestFeature;
import static app.bpartners.geojobs.endpoint.rest.model.Detection.GeoJsonDelimitationTypeEnum.PARCEL;
import static app.bpartners.geojobs.endpoint.rest.model.Feature.TypeEnum.FEATURE;
import static app.bpartners.geojobs.endpoint.rest.model.Polygon.TypeEnum.POLYGON;

import app.bpartners.geojobs.endpoint.rest.model.*;
import app.bpartners.geojobs.service.geojson.GeometryConverter;
import app.bpartners.geojobs.service.ign.IgnCadastreFeatureFetcher;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.util.List;
import java.util.function.BiFunction;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class FeaturePolygonRetriever
    implements BiFunction<Feature, Detection.GeoJsonDelimitationTypeEnum, Feature> {
  private final GeometryConverter geometryConverter;
  private final IgnCadastreFeatureFetcher ignCadastreFeatureFetcher;
  private final BuildingFinder buildingFinder;

  @Override
  public Feature apply(
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
                  buildingFinder.getBuildingMultiPolygon(point));
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
}
