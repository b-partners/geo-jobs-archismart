package app.bpartners.geojobs.service.geodata;

import app.bpartners.geojobs.service.geodata.model.MapLayerActual;
import app.bpartners.geojobs.service.geodata.model.MapLayersReachability;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class GeodataMapLayerClient {
  private final GeodataHttpClient httpClient;

  public Optional<MapLayerActual> getActualMapLayer(double latitude, double longitude) {
    return httpClient.getOrEmptyWhenNotFound(
        "/map/layers/actual", Map.of("lat", latitude, "lon", longitude), MapLayerActual.class);
  }

  public MapLayersReachability getMapLayers(
      double latitude, double longitude, boolean onlyReachable) {
    return httpClient.get(
        "/map/layers",
        Map.of("lat", latitude, "lon", longitude, "onlyReachable", onlyReachable),
        MapLayersReachability.class);
  }
}
