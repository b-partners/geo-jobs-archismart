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
import app.bpartners.geojobs.service.cacher.CacherApiClient;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

@Slf4j
class LidarApiFacadeTest {
  RestTemplate restTemplateMock = mock();
  CacherApiClient cacherApiClientMock = mock();
  LidarApiFacade subject =
      new LidarApiFacade(
          new IgnLidarApi(new IgnLidarApiConf(IGN_LIDAR_API_URL), restTemplateMock),
          new OpenSourceLidarApi(
              new OpenSourceLidarApiConf(OPEN_SOURCE_LIDAR_API_URL), restTemplateMock),
          new FallbackLidarApi(),
          new SwissBoundaryChecker(),
          new SwissLidarApi(restTemplateMock),
          new GeometrySquareMeterArea(),
          restTemplateMock,
          cacherApiClientMock);

  private static final String UPDATED_FILE_URL = "https://data.geopf.fr/dummy.laz";
  private static final String DEPRECATED_FILE_URL = "https://storage.sbg.cloud.ovh.net/dummy.laz";

  public Geometry switzerland_with_two_lidar_data_coords() {
    Coordinate[] swissCoordinates =
        new Coordinate[] {
          new Coordinate(6.220857751344101, 46.218330151666642),
          new Coordinate(6.220048781018293, 46.218321418896565),
          new Coordinate(6.219912346396907, 46.217758267190007),
          new Coordinate(6.221001698608311, 46.217607435687704),
          new Coordinate(6.220857751344101, 46.218330151666642)
        };
    return geometryFactory.createPolygon(swissCoordinates);
  }

  @SneakyThrows
  @BeforeEach
  void setUp() {
    when(cacherApiClientMock.getWithCache(any())).thenReturn(new URL(UPDATED_FILE_URL));
  }

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

  @Test
  void download_from_swiss_api_if_it_is_in_swiss_area() throws JsonProcessingException {
    ObjectMapper mapper = new ObjectMapper();
    JsonNode mockResponse =
        mapper.readTree(
            """
    {
    	"type": "FeatureCollection",
    	"timeStamp": "2026-03-25T12:12:27.781077Z",
    	"features": [
    		{
    			"assets": {
    				"swisssurface3d_2019_2506-1119_2056_5728.las.zip": {
    					"type": "application/vnd.laszip",
    					"href": "https://data.geo.admin.ch/ch.swisstopo.swisssurface3d/swisssurface3d_2019_2506-1119/swisssurface3d_2019_2506-1119_2056_5728.las.zip",
    					"created": "2021-02-23T23:21:00.666053Z",
    					"updated": "2025-01-17T09:43:08.412804Z",
    					"proj:epsg": 2056,
    					"file:checksum": "1220B5BC68CA82FF7C94DA654FE11DF680031AA37AF25576C7289E108600580D5A9F"
    				}
    			}
    		},
    		{
    			"assets": {
    				"swisssurface3d_2019_2505-1119_2056_5728.las.zip": {
    					"type": "application/vnd.laszip",
    					"href": "https://data.geo.admin.ch/ch.swisstopo.swisssurface3d/swisssurface3d_2019_2505-1119/swisssurface3d_2019_2505-1119_2056_5728.las.zip",
    					"created": "2021-02-23T23:35:43.933469Z",
    					"updated": "2025-01-17T10:12:18.022083Z",
    					"proj:epsg": 2056,
    					"file:checksum": "1220E8BE2C5F50B61B0A2CE72D65B3A6D2AF095D7FEAC0083FC33F2961D063DB1EC1"
    				}
    			}
    		},
    		{
    			"assets": {
    				"swisssurface3d_2025_2505-1119_2056_5728.copc.laz": {
    					"type": "application/vnd.laszip+copc",
    					"href": "https://data.geo.admin.ch/ch.swisstopo.swisssurface3d/swisssurface3d_2025_2505-1119/swisssurface3d_2025_2505-1119_2056_5728.copc.laz",
    					"created": "2025-11-05T12:54:04.328340Z",
    					"updated": "2025-12-15T10:39:06.456943Z",
    					"proj:epsg": 2056,
    					"file:checksum": "12207DC83078F1D078BCEA8EE4FB658E006119AB77F5167136FA1B98E9670E832157"
    				}
    			}
    		},
    		{
    			"assets": {
    				"swisssurface3d_2025_2506-1119_2056_5728.copc.laz": {
    					"type": "application/vnd.laszip+copc",
    					"href": "https://data.geo.admin.ch/ch.swisstopo.swisssurface3d/swisssurface3d_2025_2506-1119/swisssurface3d_2025_2506-1119_2056_5728.copc.laz",
    					"created": "2025-11-05T12:55:00.364617Z",
    					"updated": "2025-12-15T10:39:56.636597Z",
    					"proj:epsg": 2056,
    					"file:checksum": "12201765A76932AABFC858A631DFC0D7D2D57AB2EC33558665B6C113550B72E3ED5D"
    				}
    			}
    		}
    	]
    }
""");
    when(restTemplateMock.getForObject(any(URI.class), eq(JsonNode.class)))
        .thenReturn(mockResponse);

    var actual = subject.getUniqueLidarFilesUrls(Set.of(switzerland_with_two_lidar_data_coords()));

    assertTrue(
        actual.containsKey(
            "https://data.geo.admin.ch/ch.swisstopo.swisssurface3d/swisssurface3d_2025_2505-1119/swisssurface3d_2025_2505-1119_2056_5728.copc.laz"));
    assertTrue(
        actual.containsKey(
            "https://data.geo.admin.ch/ch.swisstopo.swisssurface3d/swisssurface3d_2025_2506-1119/swisssurface3d_2025_2506-1119_2056_5728.copc.laz"));
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
