package app.bpartners.geojobs.service.lidar.api;

import static app.bpartners.geojobs.conf.EnvConf.*;
import static app.bpartners.geojobs.model.geometry.GeometryFactory.geometryFactory;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.OK;

import app.bpartners.geojobs.service.GeometrySquareMeterArea;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

class LidarApiFacadeTest {
  RestTemplate restTemplateMock = mock();
  LidarApiFacade subject =
      new LidarApiFacade(
          new IgnLidarApi(new IgnLidarApiConf(IGN_LIDAR_API_URL), restTemplateMock),
          new OpenSourceLidarApi(
              new OpenSourceLidarApiConf(OPEN_SOURCE_LIDAR_API_URL), restTemplateMock),
          new FallbackLidarApi(),
          restTemplateMock,
          new GeometrySquareMeterArea());

  private static final String UPDATED_FILE_URL = "https://data.geopf.fr/dummy.laz";
  private static final String DEPRECATED_FILE_URL = "https://storage.sbg.cloud.ovh.net/dummy.laz";

  @Test
  void get_lidar_laz_file_urls_form_open_source_api_ok() {
    when(restTemplateMock.getForEntity(any(String.class), eq(FeatureCollection.class)))
        .thenReturn(openSourceApiResponse(UPDATED_FILE_URL));

    var actual = subject.getUniqueLidarFilesUrls(Set.of(geometry1(), geometry2()));

    assertTrue(actual.containsKey(UPDATED_FILE_URL));
    assertEquals(1, actual.size());
    assertEquals(2, actual.get(UPDATED_FILE_URL).size());
  }

  @Test
  void get_lidar_laz_file_urls_form_ign_api_ok() {
    when(restTemplateMock.getForEntity(any(String.class), eq(FeatureCollection.class)))
        .thenReturn(emptyResponse())
        .thenReturn(ignApiResponse(UPDATED_FILE_URL))
        .thenReturn(emptyResponse())
        .thenReturn(ignApiResponse(UPDATED_FILE_URL));

    var actual = subject.getUniqueLidarFilesUrls(Set.of(geometry1(), geometry2()));

    assertTrue(actual.containsKey(UPDATED_FILE_URL));
    assertEquals(1, actual.size());
    assertEquals(2, actual.get(UPDATED_FILE_URL).size());
  }

  @Test
  void should_use_fallback_if_deprecated_data() {
    when(restTemplateMock.getForEntity(any(String.class), eq(FeatureCollection.class)))
        .thenReturn(emptyResponse())
        .thenReturn(ignApiResponse(DEPRECATED_FILE_URL));

    var headers = mock(HttpHeaders.class);
    when(headers.getContentLength()).thenReturn(20_000_000L);
    when(restTemplateMock.headForHeaders(DEPRECATED_FILE_URL)).thenReturn(headers);

    var actual = subject.getUniqueLidarFilesUrls(Set.of(geometry1()));
    var expectedFallbackUrl =
        "https://data.geopf.fr/telechargement/download/LiDARHD-NUALID/NUALHD_1-0__LAZ_LAMB93_KA_2025-07-22/LHD_FXX_0644_6859_PTS_LAMB93_IGN69.copc.laz";

    assertTrue(actual.containsKey(expectedFallbackUrl));
    assertEquals(1, actual.size());
  }

  @Test
  void should_not_use_fallback_if_not_deprecated_data_even_deprecated_url() {
    when(restTemplateMock.getForEntity(any(String.class), eq(FeatureCollection.class)))
        .thenReturn(emptyResponse())
        .thenReturn(ignApiResponse(DEPRECATED_FILE_URL));

    var headers = mock(HttpHeaders.class);
    when(headers.getContentLength()).thenReturn(200_000_000L);
    when(restTemplateMock.headForHeaders(DEPRECATED_FILE_URL)).thenReturn(headers);

    var actual = subject.getUniqueLidarFilesUrls(Set.of(geometry1()));

    assertTrue(actual.containsKey(DEPRECATED_FILE_URL));
    assertEquals(1, actual.size());
  }

  @Test
  void download_should_return_correct_file() {
    when(restTemplateMock.getForObject(any(String.class), eq(byte[].class)))
        .thenReturn(new byte[] {1, 2, 3});

    var actual = subject.download(UPDATED_FILE_URL);

    assertTrue(actual.isPresent());
  }

  @Test
  void download_should_return_empty_if_not_found() {
    when(restTemplateMock.getForObject(any(String.class), eq(byte[].class)))
        .thenThrow(mock(HttpClientErrorException.NotFound.class));

    var actual = subject.download(UPDATED_FILE_URL);

    assertTrue(actual.isEmpty());
  }

  private static ResponseEntity<FeatureCollection> emptyResponse() {
    return new ResponseEntity<>(FeatureCollection.builder().features(List.of()).build(), OK);
  }

  private static ResponseEntity<FeatureCollection> ignApiResponse(String url) {
    return new ResponseEntity<>(
        FeatureCollection.builder()
            .features(
                List.of(FeatureCollection.Feature.builder().properties(Map.of("url", url)).build()))
            .build(),
        OK);
  }

  private static ResponseEntity<FeatureCollection> openSourceApiResponse(String url) {
    return new ResponseEntity<>(
        FeatureCollection.builder()
            .features(
                List.of(
                    FeatureCollection.Feature.builder()
                        .assets(new FeatureCollection.Feature.Assets(Map.of("href", url)))
                        .build()))
            .build(),
        OK);
  }

  private static Geometry geometry1() {
    var roof1Coordinates =
        new Coordinate[] {
          new Coordinate(2.243891733457616, 48.82448842864014),
          new Coordinate(2.243947393505863, 48.82437718542337),
          new Coordinate(2.244038835011281, 48.82440597780899),
          new Coordinate(2.2440209442821413, 48.82445309258651),
          new Coordinate(2.244197863717403, 48.8244975898354),
          new Coordinate(2.24422768160008, 48.82447010624497),
          new Coordinate(2.24432906240051, 48.824487119898066),
          new Coordinate(2.244263463059525, 48.82456695311532),
          new Coordinate(2.243891733457616, 48.82448842864014)
        };
    return geometryFactory.createPolygon(roof1Coordinates);
  }

  private static Geometry geometry2() {
    var coordinates =
        new Coordinate[] {
          new Coordinate(2.243891733457616, 48.82448842864014),
          new Coordinate(2.243947393505863, 48.82437718542337),
          new Coordinate(2.244263463059525, 48.82456695311532),
          new Coordinate(2.243891733457616, 48.82448842864014)
        };
    return geometryFactory.createPolygon(coordinates);
  }
}
