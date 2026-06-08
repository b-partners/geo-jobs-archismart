package app.bpartners.geojobs.service.osm;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

public class OverpassClient {

  private static final HttpClient client = HttpClient.newHttpClient();
  private static final String ENDPOINT = "https://overpass-api.de/api/interpreter";

  public static String buildingsAround(double lat, double lon, int radiusMeters) {
    String query =
        String.format(
            java.util.Locale.US,
            """
            [out:json][timeout:25];
            (
              way["building"](around:%d,%f,%f);
              relation["building"](around:%d,%f,%f);
            );
            out geom;
            """,
            radiusMeters,
            lat,
            lon,
            radiusMeters,
            lat,
            lon);

    HttpRequest request =
        HttpRequest.newBuilder()
            .uri(URI.create(ENDPOINT))
            .header("User-Agent", "birdia/1.0 (tech@birdia.fr)")
            .header("Accept", "application/json")
            .header("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8")
            .POST(
                HttpRequest.BodyPublishers.ofString(
                    "data=" + URLEncoder.encode(query, StandardCharsets.UTF_8)))
            .timeout(Duration.ofSeconds(30))
            .build();

    HttpResponse<String> response;
    try {
      response = client.send(request, HttpResponse.BodyHandlers.ofString());
    } catch (IOException | InterruptedException e) {
      throw new RuntimeException(e);
    }

    return response.body(); // format Overpass JSON, à convertir en GeoJSON
  }
}
