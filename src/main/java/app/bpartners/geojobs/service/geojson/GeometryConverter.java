package app.bpartners.geojobs.service.geojson;

import static app.bpartners.geojobs.endpoint.rest.model.Feature.TypeEnum.FEATURE;
import static app.bpartners.geojobs.endpoint.rest.model.Geometry.TypeEnum.MULTI_POLYGON;
import static app.bpartners.geojobs.endpoint.rest.model.Geometry.TypeEnum.POINT;
import static app.bpartners.geojobs.endpoint.rest.model.Polygon.TypeEnum.POLYGON;
import static app.bpartners.geojobs.model.CustomObjectMapper.objectMapper;
import static app.bpartners.geojobs.repository.model.detection.DetectableType.*;

import app.bpartners.geojobs.endpoint.rest.controller.mapper.FeatureMapper;
import app.bpartners.geojobs.endpoint.rest.model.FeatureGeometry;
import app.bpartners.geojobs.model.exception.NotImplementedException;
import app.bpartners.geojobs.repository.model.Feature;
import app.bpartners.geojobs.repository.model.detection.DetectableType;
import app.bpartners.geojobs.service.ign.IgnCadastreFeatureFetcher;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.StringReader;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.util.*;
import java.util.function.BinaryOperator;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.geotools.geojson.geom.GeometryJSON;
import org.locationtech.jts.geom.*;
import org.locationtech.jts.geom.util.GeometryFixer;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

// Most ChatGPT-generated code
@Component
@Slf4j
public class GeometryConverter {
  private final GeometryFactory geometryFactory = new GeometryFactory();

  public Feature toFeature(
      String featureId, Integer zoom, Map<String, Object> properties, MultiPolygon multiPolygon) {
    return Feature.builder()
        .id(featureId)
        .zoom(zoom)
        .geometry(
            Feature.FeatureGeometry.builder()
                .geometryType(MULTI_POLYGON)
                .actualInstanceStringValue(writeGeometryAsString(multiPolygon))
                .build())
        .properties(new HashMap<>(properties))
        .build();
  }

  @SneakyThrows
  public app.bpartners.geojobs.endpoint.rest.model.MultiPolygon restMultiPolygonFromJts(
      MultiPolygon jtsMultiPolygon) {
    var multiPolygonString = writeGeometryAsString(jtsMultiPolygon);
    return objectMapper()
        .readValue(
            multiPolygonString, app.bpartners.geojobs.endpoint.rest.model.MultiPolygon.class);
  }

  public Feature toFeature(
      String featureId, Integer zoom, Map<String, Object> properties, Point point) {
    return Feature.builder()
        .id(featureId)
        .zoom(zoom)
        .geometry(
            Feature.FeatureGeometry.builder()
                .geometryType(POINT)
                .actualInstanceStringValue(writeGeometryAsString(point))
                .build())
        .properties(new HashMap<>(properties))
        .build();
  }

  public app.bpartners.geojobs.endpoint.rest.model.Feature toRestFeature(
      org.locationtech.jts.geom.Polygon jtsPolygon) {
    return new app.bpartners.geojobs.endpoint.rest.model.Feature()
        .type(FEATURE)
        .properties(new HashMap<>())
        .geometry(
            new FeatureGeometry(
                new app.bpartners.geojobs.endpoint.rest.model.Polygon()
                    .type(POLYGON)
                    .coordinates(
                        List.of(
                            Arrays.stream(jtsPolygon.getCoordinates()).toList().stream()
                                .map(
                                    coordinate ->
                                        List.of(
                                            BigDecimal.valueOf(coordinate.getX()),
                                            BigDecimal.valueOf(coordinate.getY())))
                                .toList()))));
  }

  @SneakyThrows
  public List<BigDecimal> centroidFromGeometry(Object featureInstance) {
    ObjectMapper objectMapper = new ObjectMapper();
    Geometry geometry;
    switch (featureInstance) {
      case app.bpartners.geojobs.endpoint.rest.model.MultiPolygon multiPolygon ->
          geometry = readGeometryFromString(objectMapper.writeValueAsString(multiPolygon));
      case app.bpartners.geojobs.endpoint.rest.model.Polygon polygon ->
          geometry = readGeometryFromString(objectMapper.writeValueAsString(polygon));
      case app.bpartners.geojobs.endpoint.rest.model.Point point ->
          geometry = readGeometryFromString(objectMapper.writeValueAsString(point));
      case MultiPolygon multiPolygon -> geometry = multiPolygon;
      case Polygon polygon -> geometry = polygon;
      case Point point -> geometry = point;
      default ->
          throw new UnsupportedOperationException(
              "Unsupported feature instance: " + featureInstance);
    }
    Coordinate centroid = geometry.getCentroid().getCoordinate();
    return List.of(BigDecimal.valueOf(centroid.x), BigDecimal.valueOf(centroid.y));
  }

  public org.locationtech.jts.geom.Polygon toPolygon(
      List<List<List<List<BigDecimal>>>> multiPolygonCoordinates) {
    GeometryFactory geometryFactory = new GeometryFactory();

    List<List<BigDecimal>> firstRing = multiPolygonCoordinates.getFirst().getFirst();
    Coordinate[] ringCoords =
        firstRing.stream()
            .map(
                point ->
                    new Coordinate(point.getFirst().doubleValue(), point.getLast().doubleValue()))
            .toArray(Coordinate[]::new);

    if (!ringCoords[0].equals2D(ringCoords[ringCoords.length - 1])) {
      Coordinate[] closedRingCoords = Arrays.copyOf(ringCoords, ringCoords.length + 1);
      closedRingCoords[closedRingCoords.length - 1] = closedRingCoords[0];
      ringCoords = closedRingCoords;
    }

    LinearRing shell = geometryFactory.createLinearRing(ringCoords);
    return geometryFactory.createPolygon(shell);
  }

  public List<List<BigDecimal>> polygonToPoints(Polygon polygon) {
    List<List<BigDecimal>> result = new ArrayList<>();

    Coordinate[] coordinates = polygon.getExteriorRing().getCoordinates();

    for (Coordinate coord : coordinates) {
      List<BigDecimal> point = new ArrayList<>(2);
      point.add(BigDecimal.valueOf(coord.getX()));
      point.add(BigDecimal.valueOf(coord.getY()));
      result.add(point);
    }

    return result;
  }

  public Polygon convertToPolygon(List<List<BigDecimal>> points) {
    if (points == null || points.size() < 4) {
      throw new IllegalArgumentException("Polygon must contain at least 4 points.");
    }

    Coordinate[] coordinates =
        points.stream()
            .map(pair -> new Coordinate(pair.get(0).doubleValue(), pair.get(1).doubleValue()))
            .toArray(Coordinate[]::new);

    coordinates = ensureClosed(coordinates);

    LinearRing shell = geometryFactory.createLinearRing(coordinates);

    Polygon polygon = geometryFactory.createPolygon(shell);

    if (!polygon.isValid()) {
      var fixGeometry = GeometryFixer.fix(polygon);
      if (fixGeometry instanceof Polygon) {
        polygon = (Polygon) fixGeometry; // ou polygon.buffer(0)
      } else if (fixGeometry instanceof MultiPolygon) {
        polygon = (Polygon) fixGeometry.getGeometryN(0);
      }
    }

    return polygon;
  }

  private static Coordinate[] ensureClosed(Coordinate[] coords) {
    if (coords.length == 0) return coords;
    if (!coords[0].equals2D(coords[coords.length - 1])) {
      Coordinate[] closed = new Coordinate[coords.length + 1];
      System.arraycopy(coords, 0, closed, 0, coords.length);
      closed[coords.length] = coords[0];
      return closed;
    }
    return coords;
  }

  public MultiPolygon apply(List<List<List<List<BigDecimal>>>> multiPolygonData) {
    // multiPolygonData = List de Polygones
    Polygon[] polygons = new Polygon[multiPolygonData.size()];

    for (int i = 0; i < multiPolygonData.size(); i++) {
      List<List<List<BigDecimal>>> polygonData = multiPolygonData.get(i);
      if (polygonData.isEmpty()) {
        throw new IllegalArgumentException("Polygon must have at least one ring");
      }

      // Premier anneau = extérieur
      LinearRing shell = toLinearRing(polygonData.get(0));

      // Anneaux intérieurs = trous (optionnels)
      LinearRing[] holes = new LinearRing[Math.max(0, polygonData.size() - 1)];
      for (int j = 1; j < polygonData.size(); j++) {
        holes[j - 1] = toLinearRing(polygonData.get(j));
      }

      polygons[i] = geometryFactory.createPolygon(shell, holes);
    }

    return geometryFactory.createMultiPolygon(polygons);
  }

  public MultiPolygon unifyMultiPolygon(List<MultiPolygon> multiPolygons) {
    GeometryCollection collection =
        new GeometryCollection(multiPolygons.toArray(new Geometry[0]), geometryFactory);
    Geometry union = collection.union();
    // Cas 1 : déjà un MultiPolygon
    if (union instanceof MultiPolygon) {
      return (MultiPolygon) union;
    }

    // Cas 2 : un seul Polygon, on l'encapsule dans un MultiPolygon
    if (union instanceof Polygon) {
      return geometryFactory.createMultiPolygon(new Polygon[] {(Polygon) union});
    }
    throw new NotImplementedException("GeometryCollection not supported for now");
  }

  public List<List<List<List<BigDecimal>>>> multiPolygonToNestedList(MultiPolygon multiPolygon) {
    List<List<List<List<BigDecimal>>>> coordinates = new ArrayList<>();

    for (int i = 0; i < multiPolygon.getNumGeometries(); i++) {
      Polygon polygon = (Polygon) multiPolygon.getGeometryN(i);
      List<List<List<BigDecimal>>> polygonList = new ArrayList<>();

      // Partie extérieure (shell)
      polygonList.add(linearRingToList(polygon.getExteriorRing()));

      // Trous éventuels (holes)
      for (int j = 0; j < polygon.getNumInteriorRing(); j++) {
        polygonList.add(linearRingToList(polygon.getInteriorRingN(j)));
      }

      coordinates.add(polygonList);
    }

    return coordinates;
  }

  public List<List<List<List<BigDecimal>>>> geometryToMultiPolygonCoordinates(Geometry geometry) {
    if (geometry instanceof MultiPolygon) {
      return multiPolygonToNestedList((MultiPolygon) geometry);
    } else if (geometry instanceof Polygon polygon) {
      List<List<List<List<BigDecimal>>>> coordinates = new ArrayList<>();
      List<List<List<BigDecimal>>> polygonList = new ArrayList<>();
      // Partie extérieure (shell)
      polygonList.add(linearRingToList(polygon.getExteriorRing()));

      // Trous éventuels (holes)
      for (int j = 0; j < polygon.getNumInteriorRing(); j++) {
        polygonList.add(linearRingToList(polygon.getInteriorRingN(j)));
      }
      coordinates.add(polygonList);
      return coordinates;
    }
    throw new UnsupportedOperationException("Unsupported geometry type: " + geometry.getClass());
  }

  private List<List<BigDecimal>> linearRingToList(LineString ring) {
    List<List<BigDecimal>> coordsList = new ArrayList<>();

    for (Coordinate coord : ring.getCoordinates()) {
      List<BigDecimal> point =
          List.of(BigDecimal.valueOf(coord.getX()), BigDecimal.valueOf(coord.getY()));
      coordsList.add(point);
    }

    return coordsList;
  }

  private LinearRing toLinearRing(List<List<BigDecimal>> ringData) {
    Coordinate[] coordinates = new Coordinate[ringData.size()];
    for (int i = 0; i < ringData.size(); i++) {
      List<BigDecimal> point = ringData.get(i);
      if (point.size() < 2) {
        throw new IllegalArgumentException("Each feature must have at least 2 coordinates (x, y)");
      }
      coordinates[i] = new Coordinate(point.get(0).doubleValue(), point.get(1).doubleValue());
    }
    if (coordinates.length > 0 && !coordinates[0].equals2D(coordinates[coordinates.length - 1])) {

      Coordinate[] closed = new Coordinate[coordinates.length + 1];
      System.arraycopy(coordinates, 0, closed, 0, coordinates.length);
      closed[closed.length - 1] = coordinates[0];
      coordinates = closed;
    }
    return geometryFactory.createLinearRing(coordinates);
  }

  @SneakyThrows
  public String writeGeometryAsString(Geometry geometry) {
    GeometryJSON geometryJSON = new GeometryJSON(15);
    StringWriter writer = new StringWriter();
    geometryJSON.write(geometry, writer);
    return writer.toString();
  }

  // TODO: use it directly for others
  @SneakyThrows
  public static String staticWriteGeometryAsString(Geometry geometry) {
    GeometryJSON geometryJSON = new GeometryJSON(15);
    StringWriter writer = new StringWriter();
    geometryJSON.write(geometry, writer);
    return writer.toString();
  }

  @SneakyThrows
  public Geometry readGeometryFromString(String geoJsonString) {
    return readGeometryFromString(geoJsonString, 15);
  }

  @SneakyThrows
  public Geometry readGeometryFromString(String geoJsonString, int decimals) {
    GeometryJSON geometryJSON = new GeometryJSON(decimals);
    return geometryJSON.read(new StringReader(geoJsonString));
  }

  private double[] tileXYToLonLatBounds(int x, int y, int z) {
    double n = Math.pow(2, z);

    double lonLeft = x / n * 360.0 - 180.0;
    double lonRight = (x + 1) / n * 360.0 - 180.0;

    double latTop = Math.toDegrees(Math.atan(Math.sinh(Math.PI * (1 - 2 * y / n))));
    double latBottom = Math.toDegrees(Math.atan(Math.sinh(Math.PI * (1 - 2 * (y + 1) / n))));

    return new double[] {lonLeft, latBottom, lonRight, latTop}; // [west, south, east, north]
  }

  public MultiPolygon getMultiPolygonFromTile(int x, int y, int zoom) {
    double[] bbox = tileXYToLonLatBounds(x, y, zoom);
    double west = bbox[0];
    double south = bbox[1];
    double east = bbox[2];
    double north = bbox[3];

    GeometryFactory gf = new GeometryFactory();
    Coordinate[] coords =
        new Coordinate[] {
          new Coordinate(west, south),
          new Coordinate(west, north),
          new Coordinate(east, north),
          new Coordinate(east, south),
          new Coordinate(west, south) // fermeture du polygone
        };

    LinearRing shell = gf.createLinearRing(coords);
    Polygon polygon = gf.createPolygon(shell, null);
    return gf.createMultiPolygon(new Polygon[] {polygon});
  }

  public static BinaryOperator<MultiPolygon> unifyMultiPolygon() {
    return (multiPolygon1, multiPolygon2) -> {
      var unifiedGeometry = multiPolygon1.union(multiPolygon2).buffer(0);
      if (unifiedGeometry instanceof MultiPolygon multiPolygon) {
        return multiPolygon;
      } else if (unifiedGeometry instanceof Polygon polygon) {
        return app.bpartners.geojobs.model.geometry.GeometryFactory.geometryFactory
            .createMultiPolygon(new Polygon[] {polygon});
      }
      throw new UnsupportedOperationException("Unsupported unified geometry : " + unifiedGeometry);
    };
  }

  // Could be ROOF or PARCEL multiPolygon
  public static MultiPolygon getMultiPolygonZoneProcessed(
      app.bpartners.geojobs.endpoint.rest.model.Feature roofFeature,
      boolean isParcelDetection,
      DetectableType detectableType) {
    var roofMultiPolygon = retrieveMultiPolygonFromFeature(roofFeature);
    if (!List.of(
                TOITURE_REVETEMENT,
                PANNEAU_PHOTOVOLTAIQUE,
                MOISISSURE_NOIRCIE,
                MOISISSURE_CLAIR,
                MOISISSURE_COULEUR,
                MOISISSURE,
                USURE_IMPORTANTE,
                USURE_LEGER,
                USURE,
                OBSTACLE,
                CHEMINEE,
                HUMIDITE_INTENSE,
                HUMIDITE_CLAIR,
                HUMIDITE,
                VELUX)
            .contains(detectableType)
        && isParcelDetection) {
      var parcelFetcher = new IgnCadastreFeatureFetcher(new RestTemplate());
      var internalConverter = new GeometryConverter();
      var parcelFeaturesFromPoint = parcelFetcher.apply(roofMultiPolygon.getCentroid());
      if (parcelFeaturesFromPoint.isEmpty()) {
        log.warn("No parcel found for roofMultiPolygon {}", roofMultiPolygon.toText());
        return null;
      }
      return internalConverter.retrieveNearestParcelMultiPolygon(parcelFeaturesFromPoint);
    }
    return roofMultiPolygon;
  }

  public static MultiPolygon retrieveMultiPolygonFromFeature(
      app.bpartners.geojobs.endpoint.rest.model.Feature feature) {
    GeometryConverter geometryConverter = new GeometryConverter();
    var geometry = feature.getGeometry();
    if (geometry == null) {
      return null;
    }
    var geometryInstance = geometry.getActualInstance();
    MultiPolygon multiPolygon;
    switch (geometryInstance) {
      case app.bpartners.geojobs.endpoint.rest.model.Polygon restPolygon ->
          multiPolygon = geometryConverter.apply(List.of(restPolygon.getCoordinates()));
      case app.bpartners.geojobs.endpoint.rest.model.MultiPolygon restMultiPolygon ->
          multiPolygon = geometryConverter.apply(restMultiPolygon.getCoordinates());
      default -> multiPolygon = null;
    }
    return multiPolygon;
  }

  public static MultiPolygon getRoofMultiPolygonZoneProcessed(
      app.bpartners.geojobs.endpoint.rest.model.Feature roofFeature) {
    GeometryConverter geometryConverter = new GeometryConverter();
    var geometryInstance = roofFeature.getGeometry().getActualInstance();
    switch (geometryInstance) {
      case app.bpartners.geojobs.endpoint.rest.model.Polygon restPolygon -> {
        return geometryConverter.apply(List.of(restPolygon.getCoordinates()));
      }
      case app.bpartners.geojobs.endpoint.rest.model.MultiPolygon restMultiPolygon -> {
        return geometryConverter.apply(restMultiPolygon.getCoordinates());
      }
      default ->
          throw new IllegalStateException(
              "Unsupported geometry type for roof: " + geometryInstance);
    }
  }

  public MultiPolygon retrieveNearestParcelMultiPolygon(List<Feature> parcelFeaturesFromPoint) {
    MultiPolygon nearestRoofMultiPolygon;
    var firstParcelFeature = FeatureMapper.toRestFeature(parcelFeaturesFromPoint.getFirst());
    var parcelInstance = firstParcelFeature.getGeometry().getActualInstance();
    switch (parcelInstance) {
      case app.bpartners.geojobs.endpoint.rest.model.Polygon restPolygon -> {
        nearestRoofMultiPolygon = apply(List.of(restPolygon.getCoordinates()));
      }
      case app.bpartners.geojobs.endpoint.rest.model.MultiPolygon restMultiPolygon -> {
        nearestRoofMultiPolygon = apply(restMultiPolygon.getCoordinates());
      }
      default ->
          throw new IllegalStateException("Unsupported parcel geometry type : " + parcelInstance);
    }
    return nearestRoofMultiPolygon;
  }
}
