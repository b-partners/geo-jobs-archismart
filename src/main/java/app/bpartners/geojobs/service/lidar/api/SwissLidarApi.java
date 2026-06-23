package app.bpartners.geojobs.service.lidar.api;

import static java.util.Locale.ROOT;

import com.fasterxml.jackson.databind.JsonNode;
import java.net.URI;
import java.util.HashSet;
import java.util.Set;
import lombok.AllArgsConstructor;
import org.locationtech.jts.geom.Envelope;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Component
@AllArgsConstructor
public class SwissLidarApi implements LidarApi {
  private static final String STAC_URL = "https://data.geo.admin.ch/api/stac/v1/search";
  private static final String COLLECTION = "ch.swisstopo.swisssurface3d";
  private final RestTemplate restTemplate;

  @Override
  public Set<String> apply(Envelope envelope) {
    String intersects = buildGeoJsonPolygon(envelope);
    URI uri =
        UriComponentsBuilder.fromHttpUrl(STAC_URL)
            .queryParam("collections", COLLECTION)
            .queryParam("intersects", intersects)
            .build()
            .encode()
            .toUri();

    JsonNode data = restTemplate.getForObject(uri, JsonNode.class);
    if (data == null || !data.has("features")) {
      return Set.of();
    }

    Set<String> results = new HashSet<>();

    for (JsonNode feature : data.get("features")) {
      JsonNode assets = feature.path("assets");
      for (var it = assets.fields(); it.hasNext(); ) {
        var entry = it.next();
        if (entry.getKey().endsWith(".copc.laz")) {
          String href = entry.getValue().path("href").asText(null);
          if (href != null) {
            results.add(href);
          }
        }
      }
    }

    return results;
  }

  private String buildGeoJsonPolygon(Envelope envelope) {
    double minX = envelope.getMinX();
    double minY = envelope.getMinY();
    double maxX = envelope.getMaxX();
    double maxY = envelope.getMaxY();

    return String.format(
        ROOT,
        "{\"type\":\"Polygon\",\"coordinates\":[[[%f,%f],[%f,%f],[%f,%f],[%f,%f],[%f,%f]]]}",
        minX,
        minY,
        maxX,
        minY,
        maxX,
        maxY,
        minX,
        maxY,
        minX,
        minY);
  }
}
