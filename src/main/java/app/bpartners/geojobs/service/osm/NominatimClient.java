package app.bpartners.geojobs.service.osm;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class NominatimClient {
  private final ObjectMapper objectMapper;
  private final HttpClient client = HttpClient.newHttpClient();

  @SneakyThrows
  public String geocodeToGeoJson(String address) {
    String encoded = URLEncoder.encode(address, StandardCharsets.UTF_8);
    String url =
        "https://nominatim.openstreetmap.org/search"
            + "?q="
            + encoded
            + "&format=geojson"
            + "&polygon_geojson=1"
            + "&limit=1";

    HttpRequest request =
        HttpRequest.newBuilder()
            .uri(URI.create(url))
            .header("User-Agent", "birdia/1.0 (tech@birdia.fr)")
            .GET()
            .build();

    HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

    return response.body();
  }
}
