package app.bpartners.geojobs.service;

import static app.bpartners.geojobs.endpoint.rest.model.DelimitationObjectType.BUILDING_ROOF;
import static app.bpartners.geojobs.endpoint.rest.model.DelimitationType.PARCEL_FREE_DELIMITATION;
import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpMethod.POST;

import app.bpartners.geojobs.endpoint.rest.model.AddressFullText;
import app.bpartners.geojobs.endpoint.rest.model.ThreeDAddressesRequest;
import app.bpartners.geojobs.endpoint.rest.model.ThreeDResponseStatus;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Slf4j
@Disabled("Local use only")
class ThreeDRequestIT {
  private final RestTemplate restTemplate = new RestTemplate();
  private static final String BASE_URL = System.getenv("BASE_URL");
  private static final String API_KEY = System.getenv("API_KEY");

  @SneakyThrows
  @Test
  void create_3d_request_batch() {
    var addresses =
        List.of(
            "1 Bd du Riou - 06400 Cannes",
            "44.857200907657514, 4.829979795217078", // "D86 Ldit Les Cotes - 07800
            // Saint-Georges-les-Bains",
            "19 Rue du Four Saint-Jacques - Compiègne",
            "2 Av. Jean Jaurès - 03350 Cérilly",
            "1 Rue de la Loire - 35470 Bain-de-Bretagne",
            "23 Rue de Berlin - 13127 Vitrolles",
            "5 Rue Claude-Marie Perroud - Toulouse",
            "Zoning Industriel Moimont, 11 Rue Jean Jaurès - 95670 Marly-la-Ville",
            "2 Rte de Montpellier - 30350 Lédignan",
            "1 Fbg Saint-Nicolas - 89500 Villeneuve-sur-Yonne",
            "62 Rue Louis Delos - 59709 Marcq-en-Baroeul",
            "61 Rue de la Chapelle - 75018 Paris",
            "Pl. de la Gare - Joyeuse");

    List<Callable<Void>> callables = new ArrayList<>();
    for (var address : addresses) {
      Callable<Void> requestCallableVoid =
          () -> {
            var id = randomUUID().toString();
            log.info("3d request ID {} for address {}", id, address);
            process3dRequest(id, address);
            return null;
          };
      callables.add(requestCallableVoid);
    }

    try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
      List<Future<Void>> futures = executor.invokeAll(callables);

      for (Future<Void> future : futures) {
        assertDoesNotThrow(() -> future.get());
      }
    }
  }

  private void process3dRequest(String id, String address)
      throws InterruptedException, URISyntaxException {
    assertDoesNotThrow(
        () -> compute3dRequest(id, address),
        "Address " + address + " unable to process 3d request");

    Thread.sleep(Duration.ofSeconds(30));

    String cityJsonUrl = null;
    var attemptNb = 1;
    while (cityJsonUrl == null || attemptNb < 5) {
      var actualStatus = get3dRequestStatus(id);
      if (actualStatus.getCityJsonFileUrls() != null
          && !actualStatus.getCityJsonFileUrls().isEmpty()) {
        cityJsonUrl = actualStatus.getCityJsonFileUrls().getFirst().getUrl();
      }
      Thread.sleep(Duration.ofSeconds(30));
      attemptNb++;
    }
    downloadFile(cityJsonUrl, address + ".json");
  }

  private void compute3dRequest(String id, String address) throws URISyntaxException {
    var uri =
        UriComponentsBuilder.fromUri(new URI(BASE_URL + "/3d/" + id + "/addresses")).toUriString();

    var headers = new HttpHeaders();
    headers.add("x-api-key", API_KEY);

    restTemplate.exchange(
        uri,
        POST,
        new HttpEntity<>(
            new ThreeDAddressesRequest()
                .delimitationType(PARCEL_FREE_DELIMITATION)
                .delimitationObjectType(BUILDING_ROOF)
                .addresses(List.of(new AddressFullText().fullText(address))),
            headers),
        ThreeDResponseStatus.class);
  }

  private ThreeDResponseStatus get3dRequestStatus(String id) throws URISyntaxException {
    var uri = UriComponentsBuilder.fromUri(new URI(BASE_URL + "/3d/" + id)).toUriString();
    var headers = new HttpHeaders();
    headers.add("x-api-key", API_KEY);

    return restTemplate
        .exchange(uri, GET, new HttpEntity<>(headers), ThreeDResponseStatus.class)
        .getBody();
  }

  @SneakyThrows
  private void downloadFile(String url, String outputFileName) {
    URL fileUrl = new URI(url).toURL();
    URLConnection connection = fileUrl.openConnection();
    connection.setConnectTimeout(10_000);
    connection.setReadTimeout(30_000);

    Path path = Paths.get("city-json-archismart", outputFileName);

    try (InputStream in = connection.getInputStream()) {
      if (path.getParent() != null) {
        Files.createDirectories(path.getParent());
      }
      Files.copy(in, path, StandardCopyOption.REPLACE_EXISTING);
    }

    log.info("Downloaded here  : " + path.toAbsolutePath());
  }
}
