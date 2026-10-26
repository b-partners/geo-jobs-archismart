package app.bpartners.geojobs.service.lidar.api;

import static app.bpartners.geojobs.model.geometry.GeometryFactory.geometryFactory;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import app.bpartners.geojobs.service.GeometrySquareMeterArea;
import app.bpartners.geojobs.service.cacher.CacherApiClient;
import java.net.URI;
import java.net.URL;
import java.util.List;
import java.util.Set;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

@Slf4j
class LidarApiFacadeTest {
  RestTemplate restTemplateMock = mock();
  CacherApiClient cacherApiClientMock = mock();
  GeodataLidarApiClient geodataLidarApiClientMock = mock();
  LidarApiFacade subject =
      new LidarApiFacade(
          geodataLidarApiClientMock,
          new GeometrySquareMeterArea(),
          restTemplateMock,
          cacherApiClientMock);

  private static final String UPDATED_FILE_URL = "https://data.geopf.fr/dummy.laz";

  @SneakyThrows
  @BeforeEach
  void setUp() {
    when(cacherApiClientMock.getWithCache(any())).thenReturn(new URL(UPDATED_FILE_URL));
  }

  @Test
  void groups_geometries_by_returned_url_and_projects_to_lambert93_for_france() {
    when(geodataLidarApiClientMock.getLidarFileUrls(any()))
        .thenReturn(
            new GeodataLidarApiClient.LidarUrlsResult(
                "FRANCE",
                "EPSG:2154",
                List.of(Set.of(UPDATED_FILE_URL), Set.of(UPDATED_FILE_URL))));

    var actual = subject.getUniqueLidarFilesUrls(List.of(geometry1(), geometry2()));

    assertTrue(actual.filesUrls().containsKey(UPDATED_FILE_URL));
    assertEquals(1, actual.filesUrls().size());
    assertEquals(2, actual.filesUrls().get(UPDATED_FILE_URL).size());
    assertEquals(GeometrySquareMeterArea.LAMBERT_93, actual.targetCrs());
  }

  @Test
  void resolves_switzerland_target_crs() {
    when(geodataLidarApiClientMock.getLidarFileUrls(any()))
        .thenReturn(
            new GeodataLidarApiClient.LidarUrlsResult(
                "SWITZERLAND", "EPSG:2056", List.of(Set.of(UPDATED_FILE_URL))));

    var actual = subject.getUniqueLidarFilesUrls(List.of(geometry1()));

    assertEquals(GeometrySquareMeterArea.EPSG_2056, actual.targetCrs());
  }

  @Test
  void resolves_wallonia_target_crs() {
    when(geodataLidarApiClientMock.getLidarFileUrls(any()))
        .thenReturn(
            new GeodataLidarApiClient.LidarUrlsResult(
                "WALLONIA", "EPSG:3812", List.of(Set.of(UPDATED_FILE_URL))));

    var actual = subject.getUniqueLidarFilesUrls(List.of(geometry1()));

    assertEquals(GeometrySquareMeterArea.EPSG_3812, actual.targetCrs());
  }

  @Test
  void returns_empty_result_without_calling_geodata_when_no_geometries() {
    var actual = subject.getUniqueLidarFilesUrls(List.of());

    assertTrue(actual.filesUrls().isEmpty());
    verifyNoInteractions(geodataLidarApiClientMock);
  }

  @Test
  void download_should_return_correct_file() {
    when(restTemplateMock.getForObject(any(URI.class), eq(byte[].class)))
        .thenReturn(new byte[] {1, 2, 3});

    var actual = subject.download(UPDATED_FILE_URL);

    assertTrue(actual.isPresent());
  }

  @Test
  void download_should_return_empty_if_not_found() {
    when(restTemplateMock.getForObject(any(URI.class), eq(byte[].class)))
        .thenThrow(mock(HttpClientErrorException.NotFound.class));

    var actual = subject.download(UPDATED_FILE_URL);

    assertTrue(actual.isEmpty());
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
