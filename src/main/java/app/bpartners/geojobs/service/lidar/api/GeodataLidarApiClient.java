package app.bpartners.geojobs.service.lidar.api;

import app.bpartners.geojobs.service.geodata.GeodataHttpClient;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.SneakyThrows;
import org.locationtech.jts.geom.Geometry;
import org.springframework.stereotype.Component;

/** Calls bpartners-geodata's {@code POST /lidar/urls} to resolve LIDAR file URLs for geometries. */
@Component
public class GeodataLidarApiClient {
  private final ObjectMapper om;
  private final GeodataHttpClient httpClient;

  public GeodataLidarApiClient(ObjectMapper om, GeodataHttpClient httpClient) {
    this.om = om;
    this.httpClient = httpClient;
  }

  public record LidarUrlsResult(String region, String crs, List<Set<String>> urlsPerGeometry) {}

  @SneakyThrows
  public LidarUrlsResult getLidarFileUrls(List<Geometry> wgs84Geometries) {
    var geometriesJson = wgs84Geometries.stream().map(GeoJsonGeometryWriter::toGeoJson).toList();
    var requestBody = om.writeValueAsString(Map.of("geometries", geometriesJson));

    var body = httpClient.post("/lidar/urls", requestBody, LidarUrlsResponse.class);
    var urlsPerGeometry =
        body.results() == null
            ? List.<Set<String>>of()
            : body.results().stream()
                .map(item -> item.urls() == null ? Set.<String>of() : Set.copyOf(item.urls()))
                .toList();
    return new LidarUrlsResult(body.region(), body.crs(), urlsPerGeometry);
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  record LidarUrlsResponse(String region, String crs, List<LidarUrlsResultItem> results) {}

  @JsonIgnoreProperties(ignoreUnknown = true)
  record LidarUrlsResultItem(List<String> urls) {}
}
