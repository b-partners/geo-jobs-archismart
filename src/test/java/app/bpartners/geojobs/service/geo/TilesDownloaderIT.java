package app.bpartners.geojobs.service.geo;

import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.assertEquals;

import app.bpartners.geojobs.conf.FacadeIT;
import app.bpartners.geojobs.endpoint.rest.model.GeoServerParameter;
import app.bpartners.geojobs.endpoint.rest.model.Geometry;
import app.bpartners.geojobs.endpoint.rest.model.MultiPolygon;
import app.bpartners.geojobs.file.bucket.BucketComponent;
import app.bpartners.geojobs.repository.model.Feature;
import app.bpartners.geojobs.repository.model.ParcelContent;
import app.bpartners.geojobs.service.tiling.downloader.TilesDownloader;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

@Disabled("TODO: flaky")
class TilesDownloaderIT extends FacadeIT {
  @MockBean BucketComponent bucketComponent;
  @Autowired TilesDownloader httpApiTilesDownloader;
  @Autowired ObjectMapper om;

  private ParcelContent a_parcel_from_cannes(int zoom)
      throws MalformedURLException, JsonProcessingException {
    List<List<List<List<BigDecimal>>>> coordinates =
        List.of(
            List.of(
                List.of(
                    List.of(
                        new BigDecimal("4.825981555776383"), new BigDecimal("45.733401843736686")),
                    List.of(
                        new BigDecimal("4.826104636117987"), new BigDecimal("45.733393355437265")),
                    List.of(
                        new BigDecimal("4.826096147818566"), new BigDecimal("45.733285129619645")),
                    List.of(
                        new BigDecimal("4.825981555776383"), new BigDecimal("45.733291495844213")),
                    List.of(
                        new BigDecimal("4.825981555776383"),
                        new BigDecimal("45.733401843736686")))));

    HashMap<String, Object> properties = new HashMap<>();
    var featureId = randomUUID().toString();
    properties.put("zoom", zoom);
    properties.put("id", featureId);
    return ParcelContent.builder()
        .id(randomUUID().toString())
        .geoServerUrl(new URL("https://data.grandlyon.com/fr/geoserv/grandlyon/ows"))
        .geoServerParameter(
            om.readValue(
                """
                {
                             "service": "WMS",
                             "request": "GetMap",
                             "layers": "ortho_2018",
                             "styles": "",
                             "format": "image/jpeg",
                             "transparent": true,
                             "version": "1.3.0",
                             "width": 1024,
                             "height": 1024,
                             "srs": "EPSG:3857"
                         }""",
                GeoServerParameter.class))
        .feature(
            Feature.builder()
                .id(featureId)
                .geometry(
                    Feature.FeatureGeometry.builder()
                        .geometryType(Geometry.TypeEnum.MULTI_POLYGON)
                        .actualInstanceStringValue(
                            om.writeValueAsString(
                                new MultiPolygon()
                                    .type(MultiPolygon.TypeEnum.MULTI_POLYGON)
                                    .coordinates(coordinates)))
                        .build())
                .zoom(zoom)
                .properties(properties)
                .build())
        .build();
  }

  @Test
  void download_tiles_lyon_ok() throws IOException {
    var zoom = 20;

    var tilesDir = httpApiTilesDownloader.apply(a_parcel_from_cannes(zoom));

    assertEquals(2, new File(tilesDir.getAbsolutePath() + "/" + zoom).listFiles().length);
  }
}
