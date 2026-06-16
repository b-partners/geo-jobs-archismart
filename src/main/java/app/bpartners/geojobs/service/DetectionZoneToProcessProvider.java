package app.bpartners.geojobs.service;

import static app.bpartners.geojobs.endpoint.rest.model.DelimitationType.PARCEL;
import static app.bpartners.geojobs.endpoint.rest.model.DelimitationType.PARCEL_CONSTRAINED_DELIMITATION;
import static app.bpartners.geojobs.endpoint.rest.model.ModelName.TOITURE;
import static app.bpartners.geojobs.model.geometry.GeometryFactory.geometryFactory;
import static app.bpartners.geojobs.service.geojson.GeometryConverter.unifyMultiPolygon;

import app.bpartners.geojobs.endpoint.rest.model.*;
import app.bpartners.geojobs.repository.model.detection.Detection;
import app.bpartners.geojobs.service.geojson.GeometryConverter;
import app.bpartners.geojobs.service.geometry.JtsGeoFeature;
import app.bpartners.geojobs.service.ign.IgnCadastreFeatureFetcher;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.Polygon;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DetectionZoneToProcessProvider implements Function<Detection, MultiPolygon> {
  private final GeometryConverter geometryConverter;
  private final IgnCadastreFeatureFetcher ignCadastreFeatureFetcher;
  private final BuildingFinder buildingFinder;

  @Override
  public MultiPolygon apply(Detection detection) {
    if (detection == null) {
      return geometryFactory.createMultiPolygon(new Polygon[0]);
    }
    var jtsGeoFeatures = applyInternalGeoFeatures(detection);
    return jtsGeoFeatures.stream()
        .map(JtsGeoFeature::geometry)
        .map(
            geometry -> {
              if (geometry instanceof org.locationtech.jts.geom.Polygon polygon) {
                return geometryFactory.createMultiPolygon(new Polygon[] {polygon});
              }
              if (geometry instanceof MultiPolygon multiPolygon) {
                return multiPolygon;
              }
              return null;
            })
        .filter(Objects::nonNull)
        .reduce(unifyMultiPolygon())
        .orElseThrow(
            () ->
                new IllegalArgumentException(
                    "Unable to unify provided zone for detection.id : " + detection.getId()));
  }

  public List<JtsGeoFeature> applyInternalGeoFeatures(Detection detection) {
    if (detection == null) {
      return List.of();
    }
    var geoJsonDelimitationType = detection.getGeoJsonDelimitationType();
    var detectableObjectModelList = detection.getDetectableObjectModelList();
    var actualModelNames =
        detectableObjectModelList != null && !detectableObjectModelList.isEmpty()
            ? detectableObjectModelList.stream().map(DetectableObjectModel::getModelName).toList()
            : List.of(Objects.requireNonNull(detection.getDetectableObjectModel().getModelName()));
    var providedGeoJsonZone = detection.getProvidedGeoJsonZone();
    return apply(providedGeoJsonZone, actualModelNames, geoJsonDelimitationType);
  }

  public List<JtsGeoFeature> apply(
      List<Feature> featureList,
      List<ModelName> modelNames,
      DelimitationType geoJsonDelimitationType) {
    if (featureList == null) {
      return List.of();
    }
    if (featureList.isEmpty()) {
      return List.of();
    }
    return featureList.stream()
        .map(
            feature -> {
              var geometry = feature.getGeometry();
              var properties = feature.getProperties();
              if (geometry == null) {
                return null;
              }
              var geometryType = geometry.getActualInstance();
              return switch (geometryType) {
                case Point point -> {
                  if (modelNames.contains(TOITURE)) {
                    if (PARCEL.equals(geoJsonDelimitationType)
                        || PARCEL_CONSTRAINED_DELIMITATION.equals(geoJsonDelimitationType)) {
                      try {
                        var parcelsNearestPoint =
                            ignCadastreFeatureFetcher.apply(
                                geometryConverter.readGeometryFromString(
                                    new ObjectMapper().writeValueAsString(point)));
                        if (parcelsNearestPoint.isEmpty()) {
                          yield null;
                        }
                        if (parcelsNearestPoint.size() > 1) {
                          log.warn(
                              "More than one parcel found for point {}, used first {}",
                              point,
                              parcelsNearestPoint.getFirst());
                        }
                        yield new JtsGeoFeature(
                            properties,
                            geometryConverter.readGeometryFromString(
                                parcelsNearestPoint
                                    .getFirst()
                                    .getGeometry()
                                    .getActualInstanceStringValue()));
                      } catch (JsonProcessingException e) {
                        throw new RuntimeException(e);
                      }
                    }
                    yield new JtsGeoFeature(
                        properties, buildingFinder.getBuildingMultiPolygon(point));
                  }
                  yield null;
                }
                case app.bpartners.geojobs.endpoint.rest.model.Polygon polygon ->
                    new JtsGeoFeature(
                        properties, geometryConverter.apply(List.of(polygon.getCoordinates())));
                case app.bpartners.geojobs.endpoint.rest.model.MultiPolygon multiPolygon ->
                    new JtsGeoFeature(
                        properties, geometryConverter.apply(multiPolygon.getCoordinates()));
                default ->
                    throw new UnsupportedOperationException(
                        "Unsupported geometry type to retrieve zone to process : " + geometryType);
              };
            })
        .filter(Objects::nonNull)
        .toList();
  }
}
