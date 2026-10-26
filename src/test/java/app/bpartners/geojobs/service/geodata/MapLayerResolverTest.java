package app.bpartners.geojobs.service.geodata;

import static java.time.Instant.now;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import app.bpartners.geojobs.model.exception.GatewayTimeoutException;
import app.bpartners.geojobs.repository.CachedAreaPictureMapLayerRepository;
import app.bpartners.geojobs.repository.model.CachedAreaPictureMapLayer;
import app.bpartners.geojobs.service.geodata.model.GeodataMapLayer;
import app.bpartners.geojobs.service.geodata.model.MapLayerActual;
import java.time.Duration;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class MapLayerResolverTest {
  private static final double LONGITUDE = -0.2494458;
  private static final double LATITUDE = 46.65203;
  private static final String EXPECTED_CACHE_KEY = "-0.249,46.652";

  private final GeodataMapLayerClient mapLayerClientMock = mock();
  private final CachedAreaPictureMapLayerRepository cachedMapLayerRepositoryMock = mock();
  private final MapLayerResolver subject =
      new MapLayerResolver(mapLayerClientMock, cachedMapLayerRepositoryMock);

  private final GeodataMapLayer retrievedLayer =
      new GeodataMapLayer(
          "layer-id", "ALPES-MARITIMES_2024_5cm", 2024, "GEOSERVER", "06", "HOUSES_0", 5);

  @Test
  void calls_geodata_and_caches_the_whole_layer_on_a_miss() {
    when(cachedMapLayerRepositoryMock.findById(EXPECTED_CACHE_KEY)).thenReturn(Optional.empty());
    when(mapLayerClientMock.getActualMapLayer(LATITUDE, LONGITUDE))
        .thenReturn(Optional.of(new MapLayerActual("https://wms", retrievedLayer)));

    var actual = subject.apply(LONGITUDE, LATITUDE).orElseThrow();

    assertEquals(retrievedLayer, actual);
    var cachedCaptor = ArgumentCaptor.forClass(CachedAreaPictureMapLayer.class);
    verify(cachedMapLayerRepositoryMock).save(cachedCaptor.capture());
    var cached = cachedCaptor.getValue();
    assertEquals(EXPECTED_CACHE_KEY, cached.getId());
    assertEquals("ALPES-MARITIMES_2024_5cm", cached.getLayerName());
    assertEquals(5, cached.getPrecisionLevelInCm());
    assertEquals(2024, cached.getLayerYear());
    assertEquals("GEOSERVER", cached.getLayerSource());
  }

  @Test
  void does_not_call_geodata_on_a_hit_and_restores_year_and_source() {
    when(cachedMapLayerRepositoryMock.findById(EXPECTED_CACHE_KEY))
        .thenReturn(Optional.of(cachedLayerAgedOf(Duration.ofDays(1))));

    var actual = subject.apply(LONGITUDE, LATITUDE).orElseThrow();

    assertEquals("ALPES-MARITIMES_2024_5cm", actual.name());
    assertEquals(5, actual.precisionLevelInCm());
    assertEquals(2024, actual.year());
    assertEquals("GEOSERVER", actual.source());
    verifyNoInteractions(mapLayerClientMock);
    verify(cachedMapLayerRepositoryMock, never()).save(any());
  }

  @Test
  void shares_one_cache_entry_between_coordinates_of_the_same_rounded_cell() {
    assertEquals(
        MapLayerResolver.cacheKeyOf(LONGITUDE, LATITUDE),
        MapLayerResolver.cacheKeyOf(LONGITUDE + 0.0003, LATITUDE + 0.0002));
    assertFalse(
        MapLayerResolver.cacheKeyOf(LONGITUDE, LATITUDE)
            .equals(MapLayerResolver.cacheKeyOf(LONGITUDE + 0.01, LATITUDE)));
  }

  @Test
  void calls_geodata_again_once_the_cached_layer_expired() {
    when(cachedMapLayerRepositoryMock.findById(EXPECTED_CACHE_KEY))
        .thenReturn(
            Optional.of(cachedLayerAgedOf(MapLayerResolver.CACHE_ENTRY_LIFETIME.plusDays(1))));
    when(mapLayerClientMock.getActualMapLayer(LATITUDE, LONGITUDE))
        .thenReturn(Optional.of(new MapLayerActual("https://wms", retrievedLayer)));

    var actual = subject.apply(LONGITUDE, LATITUDE).orElseThrow();

    assertEquals("ALPES-MARITIMES_2024_5cm", actual.name());
    verify(mapLayerClientMock, times(1)).getActualMapLayer(LATITUDE, LONGITUDE);
    verify(cachedMapLayerRepositoryMock, times(1)).save(any());
  }

  @Test
  void returns_empty_and_caches_nothing_when_geodata_has_no_layer() {
    when(cachedMapLayerRepositoryMock.findById(EXPECTED_CACHE_KEY)).thenReturn(Optional.empty());
    when(mapLayerClientMock.getActualMapLayer(LATITUDE, LONGITUDE)).thenReturn(Optional.empty());

    assertTrue(subject.apply(LONGITUDE, LATITUDE).isEmpty());
    verify(cachedMapLayerRepositoryMock, never()).save(any());
  }

  @Test
  void does_not_cache_a_layer_without_precision() {
    when(cachedMapLayerRepositoryMock.findById(EXPECTED_CACHE_KEY)).thenReturn(Optional.empty());
    var withoutPrecision = new GeodataMapLayer("id", "layer", 2024, "GEOSERVER", null, null, null);
    when(mapLayerClientMock.getActualMapLayer(LATITUDE, LONGITUDE))
        .thenReturn(Optional.of(new MapLayerActual("https://wms", withoutPrecision)));

    assertEquals(Optional.of(withoutPrecision), subject.apply(LONGITUDE, LATITUDE));
    verify(cachedMapLayerRepositoryMock, never()).save(any());
  }

  @Test
  void propagates_geodata_unavailability() {
    when(cachedMapLayerRepositoryMock.findById(EXPECTED_CACHE_KEY)).thenReturn(Optional.empty());
    when(mapLayerClientMock.getActualMapLayer(LATITUDE, LONGITUDE))
        .thenThrow(new GatewayTimeoutException("geodata down"));

    assertThrows(GatewayTimeoutException.class, () -> subject.apply(LONGITUDE, LATITUDE));
  }

  private CachedAreaPictureMapLayer cachedLayerAgedOf(Duration age) {
    return CachedAreaPictureMapLayer.builder()
        .id(EXPECTED_CACHE_KEY)
        .layerName("ALPES-MARITIMES_2024_5cm")
        .precisionLevelInCm(5)
        .layerYear(2024)
        .layerSource("GEOSERVER")
        .cachedAt(now().minus(age))
        .build();
  }
}
