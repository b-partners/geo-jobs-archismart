package app.bpartners.geojobs.service;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import app.bpartners.geojobs.endpoint.rest.model.Feature;
import java.net.URI;
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
import org.springframework.http.HttpMethod;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Slf4j
@Disabled("TODO: local use only")
class GeoCodeIT {

  final RestTemplate restTemplate = new RestTemplate();

  @SneakyThrows
  @Test
  void attempt_hundred_address_geo_coding() {
    HttpHeaders headers = new HttpHeaders();
    headers.set("x-api-key", System.getenv("API_KEY"));

    var requestEntity = new HttpEntity<>(headers);

    var uri1 =
        UriComponentsBuilder.fromUri(new URI(System.getenv("API_URL") + "/geocode"))
            .queryParam("address", "1 rue de la vau saint jacques parthenay")
            .build()
            .encode()
            .toUri();

    var uri2 =
        UriComponentsBuilder.fromUri(new URI(System.getenv("API_URL") + "/geocode"))
            .queryParam("address", "238 Rue Pierre Mauroy")
            .build()
            .encode()
            .toUri();

    try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {

      List<Callable<Feature>> tasks = new ArrayList<>();

      for (int i = 0; i < 50; i++) {
        tasks.add(
            () ->
                restTemplate
                    .exchange(uri1, HttpMethod.GET, requestEntity, Feature.class)
                    .getBody());
        tasks.add(
            () ->
                restTemplate
                    .exchange(uri2, HttpMethod.GET, requestEntity, Feature.class)
                    .getBody());
      }

      List<Future<Feature>> futures = executor.invokeAll(tasks);

      for (Future<Feature> future : futures) {
        assertNotNull(future.get());
      }
    }
  }
}
