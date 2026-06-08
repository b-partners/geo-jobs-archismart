package app.bpartners.geojobs.service.osm;

import static app.bpartners.geojobs.model.geometry.GeometryFactory.geometryFactory;

import app.bpartners.geojobs.endpoint.rest.model.Feature;
import app.bpartners.geojobs.model.geometry.RoofDetails;
import app.bpartners.geojobs.service.geojson.GeometryConverter;
import app.bpartners.geojobs.service.osm.model.BuildingMatch;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.*;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.Nullable;
import org.locationtech.jts.geom.*;
import org.locationtech.jts.operation.distance.DistanceOp;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class OsmBuildingFinder {
  private static final int RADIUS_METERS = 30;
  private final NominatimClient nominatimClient;
  private final GeometryConverter geometryConverter;
  private final ObjectMapper objectMapper;

  @SneakyThrows
  private BuildingMatch findNearestBuilding(double lat, double lon) {
    String overpassJson = OverpassClient.buildingsAround(lat, lon, RADIUS_METERS);

    ObjectMapper mapper = new ObjectMapper();
    JsonNode root = mapper.readTree(overpassJson);
    Point queryPoint = geometryFactory.createPoint(new Coordinate(lon, lat));

    BuildingMatch best = null;
    double bestDist = Double.MAX_VALUE;

    for (JsonNode el : root.get("elements")) {
      if (!"way".equals(el.get("type").asText())) continue;
      JsonNode geom = el.get("geometry");
      if (geom == null || !geom.isArray() || geom.size() < 3) continue;

      Coordinate[] coords = new Coordinate[geom.size() + (isClosed(geom) ? 0 : 1)];
      for (int i = 0; i < geom.size(); i++) {
        coords[i] =
            new Coordinate(geom.get(i).get("lon").asDouble(), geom.get(i).get("lat").asDouble());
      }
      if (!isClosed(geom)) coords[coords.length - 1] = coords[0];

      Polygon poly = geometryFactory.createPolygon(coords);

      // Distance en degrés via JTS, puis conversion approximative en mètres
      double distDeg = DistanceOp.distance(poly, queryPoint);
      double distMeters = degreesToMeters(distDeg, lat);

      // Si le point est DANS le polygone, distance = 0
      if (poly.contains(queryPoint)) distMeters = 0;

      if (distMeters < bestDist) {
        bestDist = distMeters;
        Map<String, String> tags = new HashMap<>();
        JsonNode tagsNode = el.get("tags");
        if (tagsNode != null) {
          tagsNode.fields().forEachRemaining(e -> tags.put(e.getKey(), e.getValue().asText()));
        }
        best = new BuildingMatch(el.get("id").asLong(), poly, tags, distMeters);
      }
    }
    return best;
  }

  private boolean isClosed(JsonNode geom) {
    JsonNode first = geom.get(0);
    JsonNode last = geom.get(geom.size() - 1);
    return first.get("lon").asDouble() == last.get("lon").asDouble()
        && first.get("lat").asDouble() == last.get("lat").asDouble();
  }

  // Conversion grossière 1° ≈ 111 320 m, ajustée par latitude pour la longitude
  private double degreesToMeters(double distDeg, double lat) {
    return distDeg * 111_320.0 * Math.cos(Math.toRadians(lat));
    // approximation suffisante pour des distances < 100m
  }

  private BuildingMatch geocodeAndFindBuilding(String address) {
    String nominatimJson = nominatimClient.geocodeToGeoJson(address);
    log.info("Parsing Nominatim response : {}", nominatimJson.replaceAll("\\s+", " "));
    try {
      JsonNode root = objectMapper.readTree(nominatimJson);
      JsonNode features = root.get("features");
      if (features.isEmpty()) {
        return null;
      }
      JsonNode feature = features.get(0);
      JsonNode geom = feature.get("geometry");
      JsonNode props = feature.get("properties");
      Geometry geometry = objectMapper.readValue(geom.toPrettyString(), Geometry.class);

      // Cas chanceux : Nominatim a déjà renvoyé un polygone de bâtiment
      if ("Polygon".equals(geom.get("type").asText())) {
        if (geometry instanceof Polygon polygon) {
          var tags = new HashMap<String, String>();
          if (props.get("display_name") != null && props.get("display_name").isTextual()) {
            tags.put("display_name", props.get("display_name").asText());
          }
          return new BuildingMatch(props.get("osm_id").asLong(), polygon, tags, null);
        }
        // construire le BuildingMatch directement depuis le GeoJSON
        // (à adapter selon ton besoin)
      }
      // Cas général : on prend le centre du geometry, et on cherche le bâtiment le plus proche
      var centroidCoordinate = geometry.getCentroid().getCoordinate();
      double lon = centroidCoordinate.x;
      double lat = centroidCoordinate.y;
      return findNearestBuilding(lat, lon);
    } catch (JsonProcessingException e) {
      return null;
    }
  }

  public Feature geocodeAddress(String address) {
    try {
      var buildingMatch = geocodeAndFindBuilding(address);
      var feature = getFeature(buildingMatch);
      if (feature != null) {
        feature.putPropertiesItem("address", address);
      }
      return feature;
    } catch (Exception e) {
      throw new RuntimeException(
          "Unable to geocode address " + address + ". Error : " + e.getMessage());
    }
  }

  @Nullable
  private Feature getFeature(BuildingMatch buildingMatch) {
    if (buildingMatch == null) {
      return null;
    } else {
      var restFeature = geometryConverter.toRestFeature(buildingMatch.geometry());
      buildingMatch.tags().forEach(restFeature::putPropertiesItem);
      return restFeature;
    }
  }

  public MultiPolygon getBuildingMultiPolygon(
      app.bpartners.geojobs.endpoint.rest.model.Point point) {
    return getBuildingMultiPolygon(point.getCoordinates());
  }

  public MultiPolygon getBuildingMultiPolygon(List<BigDecimal> coordinates) {
    var longitude = coordinates.get(0).doubleValue();
    var latitude = coordinates.get(1).doubleValue();
    var nearestBuilding = findNearestBuilding(latitude, longitude);
    var buildingFeature = getFeature(nearestBuilding);
    if (buildingFeature == null) {
      return null;
    }
    var geometryInstance = buildingFeature.getGeometry().getActualInstance();
    if (geometryInstance instanceof app.bpartners.geojobs.endpoint.rest.model.Polygon polygon) {
      return geometryConverter.apply(List.of(polygon.getCoordinates()));
    } else if (geometryInstance
        instanceof app.bpartners.geojobs.endpoint.rest.model.MultiPolygon multiPolygon) {
      return geometryConverter.apply(multiPolygon.getCoordinates());
    }
    throw new IllegalStateException(
        "Unable to convert obtained feature into MultiPolygon : " + geometryInstance.getClass());
  }

  public MultiPolygon getBuildingMultiPolygon(String address) {
    var buildingFeature = geocodeAddress(address);
    if (buildingFeature == null) {
      return null;
    }
    var geometryInstance = buildingFeature.getGeometry().getActualInstance();
    if (geometryInstance instanceof app.bpartners.geojobs.endpoint.rest.model.Polygon polygon) {
      return geometryConverter.apply(List.of(polygon.getCoordinates()));
    } else if (geometryInstance
        instanceof app.bpartners.geojobs.endpoint.rest.model.MultiPolygon multiPolygon) {
      return geometryConverter.apply(multiPolygon.getCoordinates());
    }
    throw new IllegalStateException(
        "Unable to convert obtained feature into MultiPolygon : " + geometryInstance.getClass());
  }

  public List<RoofDetails> retrieveRoofPolygonsFrom(
      List<List<BigDecimal>> lonLatPolygonCoordinates) {
    throw new UnsupportedOperationException("Not implemented yet");
  }
}
