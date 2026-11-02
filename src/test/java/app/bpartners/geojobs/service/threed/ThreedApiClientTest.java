package app.bpartners.geojobs.service.threed;

import static app.bpartners.geojobs.endpoint.rest.model.Feature.TypeEnum.FEATURE;
import static app.bpartners.geojobs.endpoint.rest.model.Polygon.TypeEnum.POLYGON;
import static app.bpartners.geojobs.endpoint.rest.model.RoofIntersectionGrade.GOOD;
import static app.bpartners.geojobs.endpoint.rest.model.RoofIntersectionKind.RIDGE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.http.HttpMethod.PUT;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import app.bpartners.geojobs.endpoint.rest.model.CreateRoofIntersections;
import app.bpartners.geojobs.endpoint.rest.model.Feature;
import app.bpartners.geojobs.endpoint.rest.model.FeatureGeometry;
import app.bpartners.geojobs.endpoint.rest.model.Polygon;
import app.bpartners.geojobs.model.exception.BadRequestException;
import app.bpartners.geojobs.service.threed.conf.ThreedApiProperties;
import app.bpartners.geojobs.service.threed.exception.ThreedApiException;
import app.bpartners.geojobs.service.threed.model.CreateLrgFromFeatureFileUrl;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

class ThreedApiClientTest {
  private static final String BASE_URL = "https://threed.example.com";
  private static final String API_KEY = "threed-key";
  private final RestTemplate restTemplate = new RestTemplate();
  private final MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
  private final ThreedApiClient subject =
      new ThreedApiClient(
          restTemplate, new ThreedApiProperties(BASE_URL, API_KEY), new ObjectMapper());

  @Test
  void should_put_the_zone_and_read_the_intersections() {
    server
        .expect(requestTo(BASE_URL + "/intersections/zone-1?apiKey=" + API_KEY))
        .andExpect(method(PUT))
        .andExpect(
            content()
                .json(
                    """
                    {"feature": {"type": "Feature", "geometry": {"type": "Polygon",
                      "coordinates": [[[2.0, 48.0], [2.1, 48.0], [2.1, 48.1], [2.0, 48.0]]]}}}
                    """))
        .andRespond(withSuccess(intersectionsJson(), APPLICATION_JSON));

    var actual = subject.computeIntersections("zone-1", createRoofIntersections());

    server.verify();
    assertEquals("zone-1", actual.getId());
    assertEquals(1, actual.getPlanes().size());
    var intersection = actual.getIntersections().getFirst();
    assertEquals(RIDGE, intersection.getKind());
    assertEquals(GOOD, intersection.getGrade());
    assertEquals(List.of(0, 1), intersection.getPlanes());
  }

  @Test
  void should_map_threed_errors() {
    server
        .expect(requestTo(BASE_URL + "/intersections/zone-1?apiKey=" + API_KEY))
        .andRespond(
            withStatus(HttpStatus.UNPROCESSABLE_ENTITY)
                .contentType(APPLICATION_JSON)
                .body(
                    """
                    {"type": "NO_LIDAR_DATA_FOUND", "detail": "No lidar under the zone"}
                    """));

    var actual =
        assertThrows(
            ThreedApiException.class,
            () -> subject.computeIntersections("zone-1", createRoofIntersections()));

    assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, actual.getStatusCode());
    assertEquals("No lidar under the zone", actual.getApiError());
  }

  @Test
  void should_send_the_threed_api_key_on_lrg() {
    server
        .expect(
            requestTo(
                BASE_URL
                    + "/lrg/building-1/feature-file?geojobs-apikey=geojobs-key&apiKey="
                    + API_KEY))
        .andExpect(method(PUT))
        .andRespond(withSuccess("{}", APPLICATION_JSON));

    subject.generate("building-1", CreateLrgFromFeatureFileUrl.builder().build(), "geojobs-key");

    server.verify();
  }

  @Test
  void should_encode_the_threed_api_key() {
    var restTemplate = new RestTemplate();
    var server = MockRestServiceServer.bindTo(restTemplate).build();
    var subject =
        new ThreedApiClient(
            restTemplate, new ThreedApiProperties(BASE_URL, "a&b=c d"), new ObjectMapper());
    server
        .expect(requestTo(BASE_URL + "/lrg/building-1/feature-file?apiKey=a%26b%3Dc%20d"))
        .andRespond(withSuccess("{}", APPLICATION_JSON));

    subject.generate("building-1", CreateLrgFromFeatureFileUrl.builder().build());

    server.verify();
  }

  @Test
  void should_refuse_an_id_that_is_a_path() {
    assertThrows(
        BadRequestException.class,
        () -> subject.computeIntersections("../lrg/x", createRoofIntersections()));
    server.verify();
  }

  static CreateRoofIntersections createRoofIntersections() {
    var ring = List.of(point(2.0, 48.0), point(2.1, 48.0), point(2.1, 48.1), point(2.0, 48.0));
    var polygon = new Polygon().type(POLYGON).coordinates(List.of(ring));
    return new CreateRoofIntersections()
        .feature(new Feature().type(FEATURE).geometry(new FeatureGeometry(polygon)));
  }

  private static List<BigDecimal> point(double longitude, double latitude) {
    return List.of(BigDecimal.valueOf(longitude), BigDecimal.valueOf(latitude));
  }

  private static String intersectionsJson() {
    return """
           {
             "id": "zone-1",
             "crs": "EPSG:2154",
             "pointsDensityPerSquareMeter": 12.5,
             "planes": [
               {"index": 0, "normal": [0.0, 0.5, 0.86], "pointsCount": 120, "rmseInMeters": 0.03,
                "slopeInDegrees": 30.0, "areaInSquareMeters": 40.0}
             ],
             "intersections": [
               {"index": 0, "planes": [0, 1], "kind": "RIDGE",
                "start": {"longitude": 2.0, "latitude": 48.0, "altitude": 60.0},
                "end": {"longitude": 2.1, "latitude": 48.0, "altitude": 60.0},
                "lengthInMeters": 7.4,
                "criteria": {"pointsCount": 120, "rmseInMeters": 0.03, "sigmaInMeters": 0.1,
                             "support": 0.7, "purity": 0.9},
                "grade": "GOOD", "failedCriteria": ["SUPPORT"]}
             ]
           }
           """;
  }
}
