package app.bpartners.geojobs.service.geodata;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import app.bpartners.geojobs.service.geodata.model.GeodataMapLayer;
import app.bpartners.geojobs.service.geodata.model.MapLayerActual;
import app.bpartners.geojobs.service.geodata.model.MapLayerReachability;
import app.bpartners.geojobs.service.geodata.model.MapLayersReachability;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class GeodataMapLayerClientTest {
  GeodataHttpClient httpClientMock = mock();
  GeodataMapLayerClient subject = new GeodataMapLayerClient(httpClientMock);
  GeodataMapLayer layer = new GeodataMapLayer("id", "name", 2024, "GEOSERVER", "75", "HOUSES_0", 5);

  @Test
  void get_actual_map_layer_ok() {
    var expected = new MapLayerActual("https://wms", layer);
    when(httpClientMock.getOrEmptyWhenNotFound(
            "/map/layers/actual", Map.of("lat", 48.5, "lon", 2.3), MapLayerActual.class))
        .thenReturn(Optional.of(expected));

    assertEquals(Optional.of(expected), subject.getActualMapLayer(48.5, 2.3));
  }

  @Test
  void get_actual_map_layer_is_empty_when_not_covered() {
    when(httpClientMock.getOrEmptyWhenNotFound(
            "/map/layers/actual", Map.of("lat", 0.0, "lon", 0.0), MapLayerActual.class))
        .thenReturn(Optional.empty());

    assertTrue(subject.getActualMapLayer(0.0, 0.0).isEmpty());
  }

  @Test
  void get_map_layers_ok() {
    var expected =
        new MapLayersReachability(
            "https://wms", List.of(new MapLayerReachability(layer, true)), layer);
    when(httpClientMock.get(
            "/map/layers",
            Map.of("lat", 48.5, "lon", 2.3, "onlyReachable", true),
            MapLayersReachability.class))
        .thenReturn(expected);

    assertEquals(expected, subject.getMapLayers(48.5, 2.3, true));
  }
}
