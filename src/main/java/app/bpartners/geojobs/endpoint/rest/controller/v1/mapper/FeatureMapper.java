package app.bpartners.geojobs.endpoint.rest.controller.v1.mapper;

import static app.bpartners.geojobs.endpoint.rest.model.Feature.TypeEnum.FEATURE;
import static app.bpartners.geojobs.endpoint.rest.model.MultiPolygon.TypeEnum.MULTI_POLYGON;
import static app.bpartners.geojobs.model.CustomObjectMapper.objectMapper;
import static app.bpartners.geojobs.model.geometry.GeometryFactory.geometryFactory;
import static java.time.Instant.now;
import static java.util.Objects.requireNonNull;

import app.bpartners.geojobs.endpoint.rest.model.*;
import app.bpartners.geojobs.model.exception.NotImplementedException;
import app.bpartners.geojobs.repository.model.Parcel;
import app.bpartners.geojobs.repository.model.ParcelContent;
import app.bpartners.geojobs.repository.model.tiling.ParcelTilingTask;
import app.bpartners.geojobs.service.BuildingFinder;
import app.bpartners.geojobs.service.geojson.GeometryConverter;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.net.URL;
import java.util.*;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.locationtech.jts.geom.Coordinate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class FeatureMapper {
  private static final Logger log = LoggerFactory.getLogger(FeatureMapper.class);
  private final GeometryConverter geometryConverter;
  private final BuildingFinder buildingFinder;

  public Parcel toDomainPolygon(
      String parcelId, Feature rest, URL geoServerUrl, GeoServerParameter GeoServerParameter) {
    var id =
        requireNonNull(rest.getProperties()).get("id") == null
            ? null
            : rest.getProperties().get("id").toString();
    return Parcel.builder()
        .id(parcelId)
        .parcelContent(
            ParcelContent.builder()
                .id(id)
                .feature(toDomainFeature(rest))
                .geoServerUrl(geoServerUrl)
                .geoServerParameter(GeoServerParameter)
                .creationDatetime(now())
                .build())
        .build();
  }

  public static Feature from(ParcelTilingTask domainTask) {
    return domainTask.getParcelContent() == null
        ? null
        : domainTask.getParcelContent().restFeatures();
  }

  public static app.bpartners.geojobs.repository.model.Feature toDomainFeature(Feature rest) {
    HashMap<String, Object> properties =
        rest.getProperties() == null ? new HashMap<>() : new HashMap<>(rest.getProperties());
    return toDomainFeature(rest, properties);
  }

  public static app.bpartners.geojobs.repository.model.Feature toDomainFeature(
      Feature rest, Map<String, Object> properties) {
    var featureId =
        properties.get("feature_id") == null ? null : properties.get("feature_id").toString();
    var id = properties.get("id") == null ? featureId : properties.get("id").toString();
    return app.bpartners.geojobs.repository.model.Feature.builder()
        .id(id)
        .zoom(properties.get("zoom") == null ? null : (Integer) properties.get("zoom"))
        .geometry(toDomainFeatureGeometry(rest.getGeometry()))
        .properties(new HashMap<>(properties))
        .build();
  }

  public static Feature toRestFeature(app.bpartners.geojobs.repository.model.Feature domain) {
    if (domain == null || domain.getGeometry() == null) {
      return null;
    }
    var restFeatureGeometry = toRestFeatureGeometry(domain.getGeometry());
    return new Feature()
        .type(FEATURE)
        .geometry(restFeatureGeometry)
        .properties(domain.getProperties());
  }

  public static Point getCentroidRestPointFromPolygon(Feature feature) {
    Map<String, Object> properties = feature.getProperties();
    if (properties == null || properties.get("centroid") == null) {
      return null;
    }
    Point point;
    try {
      var domainCentroidPoint =
          new ObjectMapper()
              .readValue(
                  properties.get("centroid").toString(),
                  app.bpartners.geojobs.repository.model.Feature.class);
      var restFeaturePoint = toRestFeature(domainCentroidPoint);
      point = restFeaturePoint.getGeometry().getPoint();
    } catch (JsonProcessingException e) {
      throw new RuntimeException(e);
    }
    return point;
  }

  @SneakyThrows
  private static app.bpartners.geojobs.repository.model.Feature.FeatureGeometry
      toDomainFeatureGeometry(FeatureGeometry featureGeometry) {
    var actualInstance = featureGeometry.getActualInstance();
    var featureDomain =
        app.bpartners.geojobs.repository.model.Feature.FeatureGeometry.builder()
            .geometryType(getGeometryType(actualInstance))
            .actualInstanceStringValue(
                objectMapper().writeValueAsString(featureGeometry.getActualInstance()))
            .build();
    return featureDomain;
  }

  public static Geometry.TypeEnum getGeometryType(Object actualInstance) {
    var clazz = actualInstance.getClass();
    if (clazz.equals(MultiPolygon.class)) {
      return Geometry.TypeEnum.MULTI_POLYGON;
    }
    if (clazz.equals(Polygon.class)) {
      return Geometry.TypeEnum.POLYGON;
    }
    if (clazz.equals(Point.class)) {
      return Geometry.TypeEnum.POINT;
    }
    throw new IllegalArgumentException("Unknown geometry" + clazz);
  }

  @SneakyThrows
  private static FeatureGeometry toRestFeatureGeometry(
      app.bpartners.geojobs.repository.model.Feature.FeatureGeometry featureGeometry) {
    var actualInstanceStringValue = featureGeometry.getActualInstanceStringValue();
    var type = featureGeometry.getGeometryType();
    if (actualInstanceStringValue == null || type == null) {
      return null;
    }
    return switch (type) {
      case POINT ->
          new FeatureGeometry(objectMapper().readValue(actualInstanceStringValue, Point.class));
      case POLYGON ->
          new FeatureGeometry(
              objectMapper()
                  .readValue(
                      actualInstanceStringValue,
                      app.bpartners.geojobs.endpoint.rest.model.Polygon.class));
      case MULTI_POLYGON ->
          new FeatureGeometry(
              objectMapper().readValue(actualInstanceStringValue, MultiPolygon.class));
    };
  }

  // TODO: handle directly in toDomainGeometry
  public org.locationtech.jts.geom.Geometry toDomainGeometryWithMultipolygonHandler(
      Feature feature) {
    var geometryType = requireNonNull(feature.getGeometry()).getActualInstance();
    return switch (geometryType) {
      case MultiPolygon multiPolygon -> {
        var multiPolygonCoordinates = multiPolygon.getCoordinates();
        var polygons =
            new org.locationtech.jts.geom.Polygon[requireNonNull(multiPolygonCoordinates).size()];
        for (int i = 0; i < polygons.length; i++) {
          var data = multiPolygonCoordinates.get(i);
          polygons[i] = geometryConverter.toPolygon(List.of(data));
        }
        yield geometryFactory.createMultiPolygon(polygons);
      }
      default -> toDomainGeometry(feature);
    };
  }

  public org.locationtech.jts.geom.Geometry toDomainGeometry(Feature feature) {
    var geometryType = feature.getGeometry().getActualInstance();
    switch (geometryType) {
      case Point point -> {
        return buildingFinder.getBuildingMultiPolygon(point);
      }
      case Polygon polygon -> {
        var polygons = geometryConverter.convertRingsToPolygons(polygon.getCoordinates());
        return polygons.size() == 1 ? polygons.getFirst() : toMultiPolygon(polygons);
      }
      case MultiPolygon multiPolygon -> {
        return toMultiPolygon(
            multiPolygon.getCoordinates().stream()
                .map(geometryConverter::convertRingsToPolygons)
                .flatMap(Collection::stream)
                .toList());
      }
      default -> throw new NotImplementedException("Unknown geometry type " + geometryType);
    }
  }

  private org.locationtech.jts.geom.MultiPolygon toMultiPolygon(
      List<org.locationtech.jts.geom.Polygon> polygons) {
    return geometryFactory.createMultiPolygon(
        polygons.toArray(new org.locationtech.jts.geom.Polygon[0]));
  }

  public org.locationtech.jts.geom.Polygon toDomainPolygon(Feature feature) {
    var geometry = toDomainGeometry(feature);
    switch (geometry) {
      case org.locationtech.jts.geom.Polygon polygon -> {
        return polygon;
      }
      case org.locationtech.jts.geom.MultiPolygon multiPolygon -> {
        return geometryConverter.widestPolygonWithSurfaceOf(multiPolygon);
      }
      default ->
          throw new NotImplementedException(
              "Not supported geometry to convert to polygon " + geometry);
    }
  }

  public List<org.locationtech.jts.geom.Polygon> toDomainList(Feature feature) {
    var geometryType = feature.getGeometry().getActualInstance();
    var acc = new ArrayList<org.locationtech.jts.geom.Polygon>();
    switch (geometryType) {
      case Point ignored ->
          throw new NotImplementedException(
              "Point geometry type not supported to convert to List<Polygon>");
      case Polygon polygon ->
          acc.addAll(geometryConverter.convertRingsToPolygons(polygon.getCoordinates()));
      case MultiPolygon multiPolygon -> {
        var multiPolygonCoordinates = multiPolygon.getCoordinates();
        var polygonsRetrievedFromMultiPolygon =
            multiPolygonCoordinates.stream()
                .map(geometryConverter::convertRingsToPolygons)
                .flatMap(Collection::stream)
                .toList();
        acc.addAll(polygonsRetrievedFromMultiPolygon);
      }
      default ->
          throw new NotImplementedException("Geometry type " + geometryType + " not supported");
    }
    return acc;
  }

  public Feature toRest(org.locationtech.jts.geom.Polygon domain, int zoom, String id) {
    List<List<List<List<BigDecimal>>>> multiPolygonCoordinates = new ArrayList<>();
    Coordinate[] polygonCoordinates = domain.getCoordinates();

    List<List<BigDecimal>> ringCoords =
        Arrays.stream(polygonCoordinates)
            .map(
                coord ->
                    List.of(BigDecimal.valueOf(coord.getX()), BigDecimal.valueOf(coord.getY())))
            .toList();

    List<List<List<BigDecimal>>> polygonCoords = new ArrayList<>();
    polygonCoords.add(ringCoords);
    multiPolygonCoordinates.add(polygonCoords);

    MultiPolygon multiPolygon = new MultiPolygon().coordinates(multiPolygonCoordinates);
    Feature feature = new Feature();
    feature.getProperties().put("id", id);
    feature.getProperties().put("zoom", zoom);
    multiPolygon.setType(MULTI_POLYGON);
    feature.setGeometry(new FeatureGeometry(multiPolygon));

    return feature;
  }

  public org.locationtech.jts.geom.Geometry domainToGeometry(
      app.bpartners.geojobs.repository.model.Feature domainFeature) {
    var rest = toRestFeature(domainFeature);
    return toDomainGeometry(rest);
  }

  // TODO: handle directly in domainToGeometry
  public org.locationtech.jts.geom.Geometry domainToGeometryWithMultipolygonHandler(
      app.bpartners.geojobs.repository.model.Feature domainFeature) {
    var rest = toRestFeature(domainFeature);
    return toDomainGeometryWithMultipolygonHandler(rest);
  }
}
