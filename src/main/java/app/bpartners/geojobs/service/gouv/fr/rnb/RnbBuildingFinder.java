package app.bpartners.geojobs.service.gouv.fr.rnb;

import static java.nio.charset.StandardCharsets.UTF_8;

import app.bpartners.geojobs.endpoint.rest.model.Point;
import app.bpartners.geojobs.model.exception.BuildingFootPrintSourceUnavailableException;
import app.bpartners.geojobs.model.exception.NotFoundException;
import app.bpartners.geojobs.model.geometry.RoofDetails;
import app.bpartners.geojobs.service.PolygonInsideCircleDistanceComputer;
import app.bpartners.geojobs.service.geojson.GeometryConverter;
import app.bpartners.geojobs.service.google.maps.GeoCodeApi;
import app.bpartners.geojobs.service.google.maps.GeoPosition;
import app.bpartners.geojobs.service.gouv.fr.rnb.component.Building;
import app.bpartners.geojobs.service.gouv.fr.rnb.component.BuildingClosest;
import com.google.maps.errors.ApiException;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.locationtech.jts.geom.MultiPolygon;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Slf4j
@Component
@RequiredArgsConstructor
public class RnbBuildingFinder {
  private final String rnbApiUrl = "https://rnb-api.beta.gouv.fr";
  private final RestTemplate restTemplate = new RestTemplate();
  private static final int DEFAULT_POLYGON_SIZE_IN_METERS = 100;
  private final GeometryConverter geometryConverter;
  private final PolygonInsideCircleDistanceComputer circleDistanceComputer;
  private final GeoCodeApi geoCodeApi;

  // @args `radius` must be between 0 and 1000 (meters)
  public BuildingClosest getBuildingClosest(Double latitude, Double longitude, Integer radius) {
    var endpoint = String.format("%s/api/alpha/buildings/closest/", rnbApiUrl);
    var point = latitude + "," + longitude;
    UriComponentsBuilder builder =
        UriComponentsBuilder.fromHttpUrl(endpoint)
            .queryParam("radius", radius)
            .queryParam("point", point)
            .queryParam("from", "tech@birdia.fr");

    var requestEntity = defaultRequestEntity();

    return restTemplate
        .exchange(builder.build().toUri(), HttpMethod.GET, requestEntity, BuildingClosest.class)
        .getBody();
  }

  public BuildingClosest getBuildingByNextUrl(String nextUrl) {
    UriComponentsBuilder builder =
        UriComponentsBuilder.fromHttpUrl(URLDecoder.decode(nextUrl, UTF_8))
            .queryParam("from", "tech@birdia.fr");

    var requestEntity = defaultRequestEntity();

    return restTemplate
        .exchange(builder.build().toUri(), HttpMethod.GET, requestEntity, BuildingClosest.class)
        .getBody();
  }

  public Building getNearestBuildingAt(Double longitude, Double latitude, Integer radius) {
    var nearestBuilding =
        getBuildingClosest(latitude, longitude, radius).results().stream()
            .min(Comparator.comparing(Building::distance))
            .orElseThrow(
                () ->
                    new NotFoundException(
                        "No building found at point (latitude="
                            + latitude
                            + ", longitude="
                            + longitude
                            + ") under "
                            + radius
                            + " meters radius"));
    var nearestBuildingPoint = nearestBuilding.point();
    var nearestBuildingDistanceFromPoint = nearestBuilding.distance();
    var nearestBuildingDetails = getBuildingByRnbId(nearestBuilding.rnbId());
    return new Building(
        nearestBuildingDetails.rnbId(),
        nearestBuildingDetails.status(),
        nearestBuildingPoint,
        nearestBuildingDetails.shape(),
        nearestBuildingDetails.addresses(),
        nearestBuildingDistanceFromPoint);
  }

  public Building getBuildingByRnbId(String rnbId) {
    var endpoint = String.format("%s/api/alpha/buildings/%s/", rnbApiUrl, rnbId);
    var requestEntity = defaultRequestEntity();
    return restTemplate.exchange(endpoint, HttpMethod.GET, requestEntity, Building.class).getBody();
  }

  private HttpEntity<Object> defaultRequestEntity() {
    var headers = new HttpHeaders();
    headers.add("Accept", "*/*");
    return new HttpEntity<>(headers);
  }

  public List<RoofDetails> retrieveRoofPolygonsFrom(
      List<List<BigDecimal>> lonLatPolygonCoordinates) {
    var jtsMultiPolygon = geometryConverter.apply(List.of(List.of((lonLatPolygonCoordinates))));
    var centroid = jtsMultiPolygon.getCentroid();
    var longitude = centroid.getCoordinate().x;
    var latitude = centroid.getCoordinate().y;
    var maxRadius = 1000;
    var minimumEnclosingRadius =
        circleDistanceComputer.apply(
            List.of(BigDecimal.valueOf(longitude), BigDecimal.valueOf(latitude)),
            lonLatPolygonCoordinates);
    if (minimumEnclosingRadius > maxRadius) {
      throw new UnsupportedOperationException(
          "Provided multiPolygon zone is larger than supported retrieving roof polygons radius"
              + " 1000, actual is "
              + minimumEnclosingRadius);
    }
    try {
      return getBuildingsFromCentroid(
          longitude, latitude, minimumEnclosingRadius.intValue(), jtsMultiPolygon);
    } catch (Exception e) {
      log.error("Unable to retrieve roof polygons from provided multiPolygon zone", e);
      throw new BuildingFootPrintSourceUnavailableException(
          "Unable to retrieve building roof polygons inside provided polygon : "
              + lonLatPolygonCoordinates);
    }
  }

  private List<RoofDetails> getBuildingsFromCentroid(
      double longitude, double latitude, int radius, MultiPolygon provided) {
    var buildingClosest = getBuildingClosest(latitude, longitude, radius);
    var buildingIdentifiers =
        new ArrayList<>(buildingClosest.results().stream().map(Building::rnbId).toList());
    while (buildingClosest.nextUrl() != null) {
      buildingClosest = getBuildingByNextUrl(buildingClosest.nextUrl());
      buildingIdentifiers.addAll(buildingClosest.results().stream().map(Building::rnbId).toList());
    }
    return buildingIdentifiers.stream()
        .map(this::getBuildingByRnbId)
        .map(
            building -> {
              var buildingAddresses =
                  building.addresses().stream()
                      .map(
                          buildingAddress -> {
                            StringBuilder addressBuilder = new StringBuilder();
                            if (buildingAddress.streetNumber() != null) {
                              addressBuilder.append(buildingAddress.streetNumber()).append(" ");
                            }
                            if (buildingAddress.streetRep() != null
                                && !buildingAddress.streetRep().isEmpty()) {
                              addressBuilder.append(buildingAddress.streetRep()).append(" ");
                            }
                            if (buildingAddress.street() != null) {
                              addressBuilder.append(buildingAddress.street()).append(" ");
                            }
                            if (buildingAddress.cityName() != null) {
                              addressBuilder.append(buildingAddress.cityName()).append(" ");
                            }
                            if (buildingAddress.cityZipCode() != null) {
                              addressBuilder.append(buildingAddress.cityZipCode()).append(" ");
                            }
                            return addressBuilder.toString().trim();
                          })
                      .toList();
              var geometryType = building.shape().getType();
              MultiPolygon geometry;
              switch (geometryType) {
                case POLYGON -> {
                  geometry =
                      geometryConverter.apply(List.of(building.shape().getPolygonCoordinates()));
                }
                case MULTI_POLYGON -> {
                  geometry = geometryConverter.apply(building.shape().getMultiPolygonCoordinates());
                }
                case POINT -> {
                  log.warn(
                      "No building obtain from around longitude={}, latitude={} with radius={}",
                      longitude,
                      latitude,
                      radius);
                  geometry = null;
                }
                default ->
                    throw new UnsupportedOperationException(
                        "Only POLYGON and MULTI_POLYGON can be converted to roof polygons, actual"
                            + " is "
                            + geometryType);
              }
              return new RoofDetails(geometry, buildingAddresses);
            })
        .filter(
            roofGeometry ->
                roofGeometry.latLonGeometry() != null
                    && (provided.contains(roofGeometry.latLonGeometry())
                        || provided.intersects(roofGeometry.latLonGeometry())))
        .toList();
  }

  public MultiPolygon getBuildingMultiPolygon(Point point) {
    if (point == null) {
      return null;
    }
    return getBuildingMultiPolygon(point.getCoordinates());
  }

  public MultiPolygon getBuildingMultiPolygon(List<BigDecimal> coordinates) {
    var longitude = coordinates.getFirst();
    var latitude = coordinates.getLast();
    var nearestBuilding =
        getNearestBuildingAt(
            longitude.doubleValue(), latitude.doubleValue(), DEFAULT_POLYGON_SIZE_IN_METERS);
    var multiPolygonCoordinates = nearestBuilding.shape().getMultiPolygonCoordinates();
    return geometryConverter.apply(multiPolygonCoordinates);
  }

  public MultiPolygon getBuildingMultiPolygon(String address) {
    GeoPosition geoPosition;
    try {
      geoPosition = geoCodeApi.searchGeoPositionFromAddress(address);
    } catch (IOException | InterruptedException | ApiException e) {
      throw new RuntimeException(
          "Unable to geocode address " + address + " for RNB building finder", e);
    }
    var longitude = geoPosition.longitude();
    var latitude = geoPosition.latitude();
    return getBuildingMultiPolygon(
        List.of(BigDecimal.valueOf(longitude), BigDecimal.valueOf(latitude)));
  }
}
