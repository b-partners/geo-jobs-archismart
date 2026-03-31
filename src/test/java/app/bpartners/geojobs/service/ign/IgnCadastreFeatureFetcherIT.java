package app.bpartners.geojobs.service.ign;

import static app.bpartners.geojobs.model.geometry.GeometryFactory.geometryFactory;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import app.bpartners.geojobs.repository.model.Feature;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.springframework.web.client.RestTemplate;

@Disabled("Local use only")
class IgnCadastreFeatureFetcherIT {
  final RestTemplate restTemplate = new RestTemplate();
  IgnCadastreFeatureFetcher subject = new IgnCadastreFeatureFetcher(restTemplate);

  @SneakyThrows
  @Test
  void fetch_10_parallel_cadastre() {
    var point = geometryFactory.createPoint(new Coordinate(3.068299423393144, 50.63012080308718));
    try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
      List<Callable<List<Feature>>> tasks = new ArrayList<>();
      for (int i = 0; i < 100; i++) {
        tasks.add(() -> subject.apply(point));
      }
      List<Future<List<Feature>>> futures = executor.invokeAll(tasks);
      for (Future<List<Feature>> future : futures) {
        assertNotNull(future.get());
      }
    }
  }
}
