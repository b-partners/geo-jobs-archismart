package app.bpartners.geojobs.service.gouv.fr.rnb;

import static java.nio.charset.StandardCharsets.UTF_8;

import app.bpartners.geojobs.model.exception.NotFoundException;
import app.bpartners.geojobs.service.gouv.fr.rnb.component.Building;
import app.bpartners.geojobs.service.gouv.fr.rnb.component.BuildingClosest;
import java.net.URLDecoder;
import java.util.Comparator;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Component
public class BuildingApi {
  private final String rnbApiUrl = "https://rnb-api.beta.gouv.fr";
  private final RestTemplate restTemplate = new RestTemplate();

  // @args `radius` must be between 0 and 1000 (meters)
  public BuildingClosest getBuildingClosest(Double latitude, Double longitude, Integer radius) {
    var endpoint = String.format("%s/api/alpha/buildings/closest/", rnbApiUrl);
    var point = latitude + "," + longitude;
    UriComponentsBuilder builder =
        UriComponentsBuilder.fromHttpUrl(endpoint)
            .queryParam("radius", radius)
            .queryParam("point", point)
            .queryParam("from", "tech@birdia.fr");

    var requestEntity = defaultRequestEntity();

    return restTemplate
        .exchange(builder.build().toUri(), HttpMethod.GET, requestEntity, BuildingClosest.class)
        .getBody();
  }

  public BuildingClosest getBuildingByNextUrl(String nextUrl) {
    UriComponentsBuilder builder =
        UriComponentsBuilder.fromHttpUrl(URLDecoder.decode(nextUrl, UTF_8))
            .queryParam("from", "tech@birdia.fr");

    var requestEntity = defaultRequestEntity();

    return restTemplate
        .exchange(builder.build().toUri(), HttpMethod.GET, requestEntity, BuildingClosest.class)
        .getBody();
  }

  public Building getNearestBuildingAt(Double longitude, Double latitude, Integer radius) {
    var nearestBuilding =
        getBuildingClosest(latitude, longitude, radius).results().stream()
            .min(Comparator.comparing(Building::distance))
            .orElseThrow(
                () ->
                    new NotFoundException(
                        "No building found at point (latitude="
                            + latitude
                            + ", longitude="
                            + longitude
                            + ") under "
                            + radius
                            + " meters radius"));
    var nearestBuildingPoint = nearestBuilding.point();
    var nearestBuildingDistanceFromPoint = nearestBuilding.distance();
    var nearestBuildingDetails = getBuildingByRnbId(nearestBuilding.rnbId());
    return new Building(
        nearestBuildingDetails.rnbId(),
        nearestBuildingDetails.status(),
        nearestBuildingPoint,
        nearestBuildingDetails.shape(),
        nearestBuildingDetails.addresses(),
        nearestBuildingDistanceFromPoint);
  }

  public Building getBuildingByRnbId(String rnbId) {
    var endpoint = String.format("%s/api/alpha/buildings/%s/", rnbApiUrl, rnbId);
    var requestEntity = defaultRequestEntity();
    return restTemplate.exchange(endpoint, HttpMethod.GET, requestEntity, Building.class).getBody();
  }

  private HttpEntity<Object> defaultRequestEntity() {
    var headers = new HttpHeaders();
    headers.add("Accept", "*/*");
    return new HttpEntity<>(headers);
  }
}
