package app.bpartners.geojobs.service.gouv.fr.rnb;

import static app.bpartners.geojobs.endpoint.rest.controller.mapper.FeatureMapper.toRestFeature;
import static app.bpartners.geojobs.endpoint.rest.model.Geometry.TypeEnum.MULTI_POLYGON;
import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.*;

import app.bpartners.geojobs.service.PolygonInsideCircleDistanceComputer;
import app.bpartners.geojobs.service.geojson.GeometryConverter;
import app.bpartners.geojobs.service.google.maps.AddressValidator;
import app.bpartners.geojobs.service.google.maps.GeoCodeApi;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

@Disabled("TODO: UnknownHostException from https://rnb-api.beta.gouv.fr in GitHub CI")
@Slf4j
class RnbBuildingFinderIT {
  RnbBuildingFinder subject =
      new RnbBuildingFinder(
          new GeometryConverter(),
          new PolygonInsideCircleDistanceComputer(),
          new GeoCodeApi(System.getenv("GOOGLE_API_KEY"), new AddressValidator()));

  @Test
  void retrieve_closest_buildings() {
    double latitude = 46.651947;
    double longitude = -0.249327;
    int radius = 100;

    var actual = subject.getBuildingClosest(latitude, longitude, radius);

    assertNotNull(actual.nextUrl());
    assertNull(actual.previousUrl());
    assertNotNull(actual.results());
  }

  @SneakyThrows
  @Test
  void retrieve_nearest_building() {
    double latitude = 46.651947;
    double longitude = -0.249327;
    int radius = 100;

    var actual = subject.getNearestBuildingAt(longitude, latitude, radius);

    assertEquals(0.0, actual.distance());
    assertEquals(
        List.of(BigDecimal.valueOf(-0.249000547126667), BigDecimal.valueOf(46.65198731242363)),
        actual.point().getPointCoordinates());
    assertEquals(MULTI_POLYGON, actual.shape().getType());
    var multiPolygonCoordinates = actual.shape().getMultiPolygonCoordinates();
    assertEquals(
        List.of(BigDecimal.valueOf(-0.248858521003516), BigDecimal.valueOf(46.651871841665866)),
        multiPolygonCoordinates.getFirst().getFirst().getFirst());
    assertEquals(
        List.of(BigDecimal.valueOf(-0.248858521003516), BigDecimal.valueOf(46.651871841665866)),
        multiPolygonCoordinates.getFirst().getFirst().getLast());
  }

  @SneakyThrows
  @Test
  void geocode_address() {
    var multiPolygon =
        subject.getBuildingMultiPolygon(
            List.of(BigDecimal.valueOf(2.369347107300078), BigDecimal.valueOf(51.03112559258034)));

    var feature =
        new GeometryConverter()
            .toFeature(randomUUID().toString(), 20, new HashMap<>(), multiPolygon);
    log.info(new ObjectMapper().writeValueAsString(toRestFeature(feature)));
  }
}
