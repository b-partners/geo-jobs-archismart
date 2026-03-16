package app.bpartners.geojobs.endpoint.rest.postprocessing;

import static app.bpartners.geojobs.model.ConversionFormatType.GEO_JSON;
import static app.bpartners.geojobs.model.geometry.GeometryFactory.geometryFactory;
import static app.bpartners.geojobs.model.geometry.route.ObjectType.bati_autres;
import static java.util.stream.Collectors.toSet;
import static org.junit.jupiter.api.Assertions.*;

import app.bpartners.geojobs.endpoint.rest.postprocessing.model.LatLonPolygon;
import app.bpartners.geojobs.endpoint.rest.postprocessing.model.TiledPolygon;
import app.bpartners.geojobs.endpoint.rest.postprocessing.model.TilingConf;
import app.bpartners.geojobs.model.geometry.IntXY;
import app.bpartners.geojobs.model.geometry.VGG;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.Set;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.springframework.core.io.ClassPathResource;

@Slf4j
class DetectionBoundaryMergerTest {
  private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
  private final GeoJsonLoader geoJsonLoader = new GeoJsonLoader();
  private final TilingConf tilingConf = new TilingConf(20, 1_024);
  private final DetectionBoundaryMerger subject = new DetectionBoundaryMerger();

  @Test
  void apply_from_vgg_file() throws IOException {
    /*
     * Configuration for the vgg projection
     * imageWidth = 4096.0
     * imageHeight = 4096.0
     *
     * # Bounding box of the image (from top-left / bottom-right points)
     * minLon = 2.3450354605486723  # left
     * maxLon = 2.3467214210839416  # right
     * minLat = 48.84656688003173   # bottom
     * maxLat = 48.847466808383814  # top
     * */
    var vggFile = new ClassPathResource("/geometry/vgg/bati_4_polygons-vgg.json").getFile();
    var vggSet = objectMapper.readValue(vggFile, new TypeReference<Set<VGG>>() {});
    var vgg = vggSet.iterator().next();
    var originalSize = vgg.values().iterator().next().getRegions().size();

    Set<TiledPolygon> actual = subject.applyVgg(vgg);

    assertEquals(4, originalSize);
    assertEquals(1, actual.size());
    assertEquals(
        Set.of(
            new TiledPolygon(
                geometryFactory.createPolygon(computeExpectedVggMergedCoordinates()),
                bati_autres,
                new IntXY(2, 3),
                new TilingConf(20, 1024))),
        actual);
  }

  @SneakyThrows
  @Test
  void apply_from_geojson_file() {
    var geojsonFile = new ClassPathResource("/geometry/geojson/bati_4_polygons.geojson").getFile();
    var latLonPolygons = geoJsonLoader.apply(geojsonFile);

    Set<LatLonPolygon> unified = subject.apply(toTiledPolygon((latLonPolygons)), GEO_JSON);

    assertEquals(4, (latLonPolygons).size());
    assertEquals(1, unified.size());
  }

  @SneakyThrows
  @Test
  @Disabled("Local use only: Visualisation test")
  void merge_bati_4_polygones() {
    var geojsonFile = new ClassPathResource("/geometry/geojson/bati_4_polygons.geojson").getFile();
    var expectedMergedGeoJsonFile =
        new ClassPathResource("/geometry/geojson/bati_4_polygones_merged.geojson").getFile();

    var polygons = geoJsonLoader.apply(geojsonFile);
    var expectedMergedPolygons = geoJsonLoader.apply(expectedMergedGeoJsonFile);
    var tiledPolygons = toTiledPolygon(polygons);
    var latLonPolygonsUnified = subject.apply(tiledPolygons, GEO_JSON);
    assertNotNull(latLonPolygonsUnified);
    assertNotNull(expectedMergedPolygons);

    var geojsonUnified = new Geojson(latLonPolygonsUnified);
    assertDoesNotThrow(() -> geojsonUnified.saveAsFile("bati_4_polygones_merged.geojson"));
  }

  private Set<TiledPolygon> toTiledPolygon(Set<LatLonPolygon> polygons) {
    return polygons.stream().map(latLon -> latLon.tiledPolygon(tilingConf)).collect(toSet());
  }

  private Coordinate[] computeExpectedVggMergedCoordinates() {
    return new Coordinate[] {
      new Coordinate(1675, 3203),
      new Coordinate(1675, 3203),
      new Coordinate(1675, 3204),
      new Coordinate(1676, 3205),
      new Coordinate(1676, 3206),
      new Coordinate(1677, 3207),
      new Coordinate(1678, 3207),
      new Coordinate(3102, 3885),
      new Coordinate(3103, 3886),
      new Coordinate(3104, 3886),
      new Coordinate(3105, 3886),
      new Coordinate(3106, 3886),
      new Coordinate(3106, 3885),
      new Coordinate(3107, 3885),
      new Coordinate(3108, 3884),
      new Coordinate(3108, 3883),
      new Coordinate(3109, 3882),
      new Coordinate(3316, 2833),
      new Coordinate(3316, 2832),
      new Coordinate(3316, 2831),
      new Coordinate(3316, 2830),
      new Coordinate(3315, 2830),
      new Coordinate(3315, 2829),
      new Coordinate(3314, 2828),
      new Coordinate(3313, 2828),
      new Coordinate(3301, 2822),
      new Coordinate(3511, 2020),
      new Coordinate(3511, 2019),
      new Coordinate(3511, 2018),
      new Coordinate(3511, 2017),
      new Coordinate(3510, 2017),
      new Coordinate(3510, 2016),
      new Coordinate(3509, 2015),
      new Coordinate(3508, 2015),
      new Coordinate(2159, 1400),
      new Coordinate(2158, 1400),
      new Coordinate(2157, 1399),
      new Coordinate(2156, 1399),
      new Coordinate(2155, 1400),
      new Coordinate(2154, 1400),
      new Coordinate(2154, 1400),
      new Coordinate(2154, 1400),
      new Coordinate(2153, 1400),
      new Coordinate(809, 845),
      new Coordinate(808, 844),
      new Coordinate(807, 844),
      new Coordinate(806, 844),
      new Coordinate(805, 845),
      new Coordinate(804, 845),
      new Coordinate(804, 846),
      new Coordinate(803, 846),
      new Coordinate(803, 847),
      new Coordinate(802, 848),
      new Coordinate(580, 1675),
      new Coordinate(580, 1675),
      new Coordinate(579, 1675),
      new Coordinate(578, 1676),
      new Coordinate(577, 1676),
      new Coordinate(577, 1677),
      new Coordinate(576, 1677),
      new Coordinate(576, 1678),
      new Coordinate(575, 1679),
      new Coordinate(363, 2462),
      new Coordinate(363, 2462),
      new Coordinate(363, 2463),
      new Coordinate(363, 2464),
      new Coordinate(364, 2465),
      new Coordinate(364, 2466),
      new Coordinate(365, 2467),
      new Coordinate(366, 2467),
      new Coordinate(1675, 3203)
    };
  }
}
